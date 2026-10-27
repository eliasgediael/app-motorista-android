# Arquitetura Android proposta

Kotlin + Jetpack Compose, uma Activity, navegação, ViewModel/StateFlow. Domínio independente de Android, repositórios de dados. Room para histórico confirmado e DataStore para preferências. MVP sem backend, conta, assinatura, anúncios ou telemetria. Estas são propostas de engenharia, não regras Apex recuperadas.

Confirmar aparelhos alvo e Android mínimo. Fixar SDK, AGP, Kotlin e Compose compatíveis no início da implementação, sem deduzir versões pelo vídeo. Iniciar com módulo app e pacotes ui/domain/data/platform; modularizar depois se necessário.

## Fluxo

Entrada manual/fixture → validação → oferta normalizada → cálculo → avaliação com regras confirmadas → UI → confirmação manual de realização → histórico/receita.

Adapter futuro de plataforma é separado e desativado. O MVP não lê 99Pop. Qualquer fonte futura deve ser autorizada e alimentar o mesmo domínio sem controlar a plataforma.

## Modelos

- Offer: ID, origem MANUAL/SIMULADA, validade/timestamp, tarifa ofertada ao motorista, distância/tempo de embarque e viagem separados; campos opcionais nulos.
- RuleSet: versão, valores opcionais, escopo, PENDENTE/CONFIRMADO, evidência/data da aprovação.
- MetricResult: valor/unidade, base usada, versão da fórmula ou motivo de indisponibilidade.
- Assessment: SEM_CRITERIOS, DADOS_INSUFICIENTES, ATENDE, ATENCAO, NAO_ATENDE. As últimas três exigem política de combinação aprovada.
- TripRecord: oferta associada, NAO_CONFIRMADA/REALIZADA/NAO_REALIZADA e ação explícita. Só REALIZADA entra nas receitas.
- Shift: INATIVA/ATIVA/PAUSADA/ENCERRADA; relógio monotônico para intervalos, timestamps/fuso para histórico e recuperação de processo. Fases manuais no MVP.
- OverlayContext: contexto autorizado, retângulos proibidos, geometria recente e validade. Contexto desconhecido implica ocultar.

Dinheiro persistido em centavos, operações decimais e arredondamento explícito; metros e segundos para distância/tempo. Formatação pt-BR só na UI. Não arredondar bases antes do cálculo nem usar ponto flutuante binário para dinheiro persistido.

## Privacidade

MVP sem câmera, microfone, localização, captura de tela ou acessibilidade. Não persistir nomes, endereços ou mídia de passageiros por padrão. Dados locais, histórico opcional e apagar tudo. Excluir dados sensíveis de logs e backup automático. Exportação futura voluntária/revisável, sem upload implícito.

## Dependências da sobreposição

Separar política geométrica do controlador de janela. Permissão de sobreposição sozinha não informa app em primeiro plano nem coordenadas do X. Sem fonte legítima e confiável de contexto, manter app próprio/notificação. Não solicitar permissões sensíveis antes de estudar viabilidade.

Referências consultadas:

- https://developer.android.com/reference/android/view/WindowManager.LayoutParams
- https://developer.android.com/about/versions/12/behavior-changes-all#untrusted-touch-events
- https://support.google.com/googleplay/android-developer/answer/10964491?hl=en
- https://termos.99app.com/legal/termos/motorista/

A 99 também referencia Política de Programas Proibidos, cuja revisão específica permanece pendente. Estas fontes não autorizam por si só uma integração. Revalidar documentação quando começar o protótipo técnico.
