export type Perfil = 'ADMINISTRADOR' | 'OPERADOR' | 'MOTORISTA' | 'PASSAGEIRO'

export const perfilLabel: Record<Perfil, string> = {
  ADMINISTRADOR: 'Administrador',
  OPERADOR: 'Operador',
  MOTORISTA: 'Motorista',
  PASSAGEIRO: 'Passageiro',
}

/** Espelha UsuarioResponse. */
export type UsuarioSessao = {
  id: string
  nome: string
  email: string
  perfil: Perfil
}

export type LoginRequest = {
  email: string
  senha: string
}
