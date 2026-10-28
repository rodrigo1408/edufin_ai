import { Compass } from 'lucide-react'
import { ButtonLink, EmptyState } from '../components/ui'

export function NotFoundPage() {
  return (
    <EmptyState
      icon={<Compass size={24} />}
      title="Página não encontrada"
      description="O endereço pode ter mudado ou não existir mais."
      action={<ButtonLink to="/painel">Voltar ao painel</ButtonLink>}
    />
  )
}
