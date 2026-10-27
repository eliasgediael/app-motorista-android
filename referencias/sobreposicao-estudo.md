> Atualização visual: os parágrafos sobre preto e branco desta referência histórica foram substituídos pela direção colorida atual. As regras de segurança continuam como proposta.

# Telas e sobreposição segura

## Visual

Preto/branco/cinzas; fonte de sistema, números com unidades, bordas discretas. Sem gradientes ou brilho. Estados por texto/ícone, sem depender de cores. Propor alvos de pelo menos 48 dp; testar fontes ampliadas e contraste. Nome/marca ainda não definidos.

## Mapa de telas

1. Início: Modo manual, Sem integração 99Pop, atalhos para análise/jornada/ajustes. Dados ausentes são travessões; não exibir receita fictícia.
2. Análise manual: tarifa, km/min até embarque e viagem, origem e base explícitas. Validar antes de calcular.
3. Resultado: métricas possíveis e razões. Sem regra: Critérios não definidos. Sem custo: Lucro indisponível. Análise não confirma viagem.
4. Critérios: campos vazios, unidades, bases e confirmação de versão. Distinguir ausente, desativado e zero deliberado.
5. Jornada: iniciar/pausar/retomar/encerrar, confirmação de encerramento, tempos separados e fases manuais.
6. Histórico: vazio inicialmente, filtros, origem e status, confirmação de realização e exclusão.
7. Cartão: prévia sintética, estados, ordem/tamanho/posição. Ajustar arraste dentro do app e com veículo parado.
8. Privacidade/permissões: finalidade por recurso, desligar/apagar e comportamento ao negar.

## Sobreposição no 99Pop — proposta, sem integração

A reclamação anterior era proximidade com o X de recusa. Um canto fixo não resolve universalmente: a posição dos controles muda.

Política proposta:

1. Iniciar OCULTO. Exigir ativação voluntária, fonte de contexto autorizada, perfil de tela validado e geometria atual.
2. X/recusa, aceite, controles do sistema e outras ações viram retângulos proibidos com margem testada. Não inventar coordenadas do 99Pop.
3. Procurar área útil capaz de conter todo o cartão sem interseção com exclusões/margem.
4. Se não couber, tentar cartão compacto validado; sem espaço, ocultar e usar alternativa autorizada.
5. Mudança de orientação, escala, teclado, janela, oferta ou perda/expiração de contexto remove a janela até nova validação.
6. Nova oferta invalida o resultado anterior. Prazo do dado é independente da duração visual. Nunca mostrar resultado antigo como atual.
7. Sem cliques/gestos na plataforma e sem janela transparente de tela inteira. Não depender de toque atravessando para resolver colisão com botões.

Janela futura pequena, informativa, sem foco e sem toque durante operação; interação/configuração no app próprio. FLAG_NOT_TOUCHABLE não garante passagem de toque em todas as condições. Android pode bloquear toques e o app de destino pode esconder sobreposições: respeitar, sem contornar proteções.

Não há detector automático de X implementado. Obter contexto e retângulos legitimamente é uma dependência aberta. Se termos/testes não permitirem fonte confiável, não haverá sobreposição automática no 99Pop.

## Testes obrigatórios em aparelho

X e aceite acessíveis sem colisão; fonte ampliada; telas pequenas; rotação; múltiplas janelas; teclado; expiração; troca de app; revogação; processo encerrado; layout/versão desconhecidos. Cenário desconhecido deve ocultar. Registrar aparelho, Android, versão 99 e evidências anonimizadas. Wireframe não aprova nenhum desses testes.
