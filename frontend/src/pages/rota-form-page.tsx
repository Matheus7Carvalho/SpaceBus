import { useParams } from 'react-router'
import { PageHeader } from '@/components/shared/page-header'
import { RegistroLoader } from '@/components/shared/registro-loader'
import { RotaForm } from '@/features/rotas/components/rota-form'
import { useRota, useSalvarRota } from '@/features/rotas/hooks'
import { paraFormulario, paraRequisicao } from '@/features/rotas/schemas'
import { useAcoesCadastro } from '@/lib/cadastro'
import { paraIsoData } from '@/lib/datas'
import { useAgora } from '@/lib/use-agora'

export function RotaFormPage() {
  const { id } = useParams()
  const hoje = paraIsoData(useAgora())
  const rota = useRota(id)
  const acoes = useAcoesCadastro({
    id,
    lista: '/cadastros/rotas',
    mutacao: useSalvarRota(id),
    paraRequisicao,
    paraFormulario: (salva) => paraFormulario(salva, hoje),
    mensagemSucesso: 'Rota salva.',
  })

  return (
    <>
      <PageHeader title={id ? 'Editar rota' : 'Nova rota'} />
      <RegistroLoader edicao={!!id} consulta={rota} naoEncontrado="Rota não encontrada.">
        <RotaForm key={rota.data?.id ?? 'nova'} rota={rota.data} defaultValues={paraFormulario(rota.data, hoje)} {...acoes} />
      </RegistroLoader>
    </>
  )
}
