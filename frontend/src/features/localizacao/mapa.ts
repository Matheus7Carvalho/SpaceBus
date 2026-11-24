/**
 * Provedor de tiles do mapa. Piloto: OpenStreetMap (volume baixo, dentro da política de uso), com atribuição.
 * Em produção, trocar por um provedor com contrato (docs/modulo-04-localizacao.md).
 */
export const TILES = {
  url: 'https://tile.openstreetmap.org/{z}/{x}/{y}.png',
  atribuicao: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>',
  zoom: 16,
  zoomMax: 19,
} as const
