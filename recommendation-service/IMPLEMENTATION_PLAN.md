# Plano de Ação — Serviço de Recomendação (MVP avançado)

Documento para guiar a implementação do serviço de recomendação, cobrindo arquitetura, backlog de entregas, decisões e trade-offs. O objetivo é um MVP avançado que possa evoluir para produção sem retrabalho estrutural.

## 1. Objetivos do MVP
- Gerar recomendações personalizadas de catálogo para cada usuário.
- Ingerir eventos de engajamento em tempo (quase) real via Kafka.
- Persistir recomendações calculadas e permitir refresh assíncrono.
- Disponibilizar API para consulta de recomendações e para forçar recomputação.

## 2. Escopo funcional (MVP)
- **Consumir eventos de engajamento**: views, likes, ratings, watch_time. Fonte: tópico Kafka `engagement-events`.
- **Ingestão de catálogo**: ler metadados do catálogo via REST do Catalog Service (fetch incremental) para features de conteúdo.
- **Geração de recomendações**:
  - Estratégia baseline: popularidade ponderada por recência e afinidade leve por gênero.
  - Personalização inicial: user-to-item baseada em contagem/tempo por gênero e criador; fallback para popularidade global.
  - Refresh assíncrono (batch curto) e endpoint de recomputação por usuário.
- **Exposição de API REST**:
  - `GET /recommendations/{userId}` com paginação e fonte cacheada.
  - `POST /recommendations/{userId}/refresh` para gatilho manual/operacional.
- **Backfill**: job para recomputar todos os usuários periodicamente (batch noturno).

## 3. Requisitos não funcionais
- **Latência**: P99 < 150 ms na leitura de recomendações cacheadas.
- **Consistência**: eventual; ingestão->recomendação dentro de SLA de minutos.
- **Disponibilidade**: meta 99% para API de leitura.
- **Observabilidade**: métricas (Prometheus), tracing (OpenTelemetry), logs estruturados.
- **Segurança**: autenticação via JWT (delegado ao API Gateway/edge) e autorização simples por userId; PII minimizada.

## 4. Arquitetura proposta
- **Pipeline de ingestão**: Kafka consumer (engagement events) → normalização → storage de eventos agregados.
- **Feature store simplificada**: tabelas agregadas por usuário (contadores, tempo assistido, gêneros mais consumidos).
- **Motor de recomendação**:
  - Batch microbatch (ex.: a cada 5 min) para recalcular top-N por usuário.
  - Fallback online simples (popularidade + gênero).
- **Cache**: tabela `recommendations` com lista ordenada por userId + TTL lógico via `updated_at`.
- **Integrações**:
  - Kafka: consumir `engagement-events`; (opcional futuro) publicar `recommendation-updated`.
  - Catalog Service: endpoint para metadados (título, gênero, elenco, idioma, tags).
  - DB: Postgres `recommendation_db`.

### Modelo de dados (inicial)
- `user_features(user_id, genre_scores jsonb, creator_scores jsonb, last_engagement_at timestamp, updated_at)`
- `item_popularity(item_id, score numeric, window_start, window_end, updated_at)`
- `recommendations(user_id, items text[], generated_at timestamp, algorithm varchar)`

## 5. Estratégia de algoritmos (evolutiva)
1. **Baseline (Semana 1-2)**: popularidade global com janela móvel + afinidade por gênero do usuário (conteúdo-based leve).
   - Trade-off: baixa personalização mas rápido e estável.
2. **Collaborative Filtering implícito (Semana 3-4)**: matriz user-item com ALS/lightFM em job batch; atualizar top-N.
   - Trade-off: requer dados mínimos e tuning; aumento de custo computacional.
3. **Híbrido (Futuro)**: blending CF + conteúdo (gênero, idioma, elenco) + regras de diversidade.
   - Trade-off: complexidade de feature store e explainability.

## 6. Backlog por fases (ações concretas)
- **Fase 0 — Fundamentos**
  - Definir schema de evento `engagement-events` (JSON) e contratos com Engagement Service.
  - Criar tabelas `user_features`, `item_popularity`, `recommendations`.
  - Instrumentar métricas básicas e logs estruturados.
- **Fase 1 — Ingestão e baseline**
  - Implementar consumer Kafka com idempotência e DLQ (tópico `engagement-events-dlq`).
  - Agregações por usuário (gênero, tempo, likes) e cálculo de popularidade por janela.
  - Job microbatch para gerar top-N e preencher `recommendations`.
  - API `GET /recommendations/{userId}` + cache DB.
- **Fase 2 — Recomputação e operabilidade**
  - Endpoint `POST /recommendations/{userId}/refresh` com fila interna para reprocesso.
  - Job de backfill completo (nightly) com limitação de throughput.
  - Alarmes: lag do consumer, erro de processamento, tempo de geração > SLA.
- **Fase 3 — Personalização CF**
  - Job batch CF implícito (Spark/Java lib) rodando off-process; escrever resultados em `recommendations`.
  - Feature flags para ligar/desligar algoritmo.
- **Fase 4 — Híbrido e qualidade**
  - Diversidade (penalizar repetição), re-ranking exploratório, A/B testing básico.

## 7. Trade-offs chave
- **Eventual vs. strong consistency**: escolhemos eventual para throughput; recomendações podem atrasar alguns minutos.
- **Batch/microbatch vs. streaming puro**: microbatch reduz complexidade e custo; streaming puro fica para fase futura com mais volume.
- **Postgres vs. store especializada**: Postgres atende MVP com facilidade operacional; migrar para Redis/Elasticsearch se latência ou carga exigirem.
- **Algoritmo simples vs. CF**: começamos simples para time-to-market; CF ativado quando houver dados suficientes.
- **Cache em DB vs. cache distribuído**: DB primeiro por simplicidade; adicionar Redis para baixar latência se necessário.

## 8. Observabilidade e qualidade
- **Métricas**: lag Kafka, throughput eventos, tempo de processamento por batch, latência de API, taxa de erros, acerto (CTR) por modelo.
- **Tracing**: OpenTelemetry nos consumidores e APIs.
- **Testes**: unidade para agregações, contrato para eventos Kafka, integração para pipeline batch.

## 9. Segurança, privacidade e conformidade
- Minimizar PII: armazenar apenas ids e sinais de consumo; evitar dados sensíveis.
- Autorização por userId na API; JWT validado no gateway.
- Criptografia em trânsito (TLS) e em repouso (config DB).
- Controles de acesso em tópicos Kafka e base de dados.

## 10. Riscos e mitigação
- **Dados insuficientes**: manter fallback de popularidade global.
- **Lag elevado no Kafka**: auto-scaling do consumer e DLQ com reprocessamento.
- **Cold start de usuários novos**: recomendações por popularidade e gênero do país/idioma padrão.

## 11. Critérios de aceite do MVP
- API de leitura retornando top-N em <150 ms (cache DB) para 95% das requisições.
- Pipeline processa novos eventos e reflete recomendações em até 5 minutos.
- Observabilidade básica ativa (métricas + logs) e documentação de operação.

