export type Perfil = 'ADMINISTRADOR' | 'OPERADOR' | 'MOTORISTA' | 'PASSAGEIRO'

export const perfilLabel: Record<Perfil, string> = {
  ADMINISTRADOR: 'Administrador',
  OPERADOR: 'Operador',
  MOTORISTA: 'Motorista',
  PASSAGEIRO: 'Passageiro',
}

/** Espelha SessaoResponse. */
export type UsuarioSessao = {
  id: string
  nome: string
  email: string
  perfil: Perfil
  /** Fuso da operação (IANA, ex.: "America/Araguaina"): "hoje" e "agora" da tela seguem o servidor. */
  fusoHorario: string
}

export type LoginRequest = {
  email: string
  senha: string
}
