import 'leaflet/dist/leaflet.css'
import { useEffect, useState } from 'react'
import { Circle, CircleMarker, MapContainer, TileLayer, useMap } from 'react-leaflet'
import { TILES } from '../mapa'

export type MapaVanProps = {
  latitude: number
  longitude: number
  precisaoM: number
  /** Posição desatualizada: o marcador continua, esmaecido. */
  esmaecido: boolean
}

/** Acompanha a posição nova sem mudar o zoom escolhido pelo usuário. */
function Seguir({ latitude, longitude }: { latitude: number; longitude: number }) {
  const map = useMap()
  useEffect(() => {
    map.panTo([latitude, longitude])
  }, [map, latitude, longitude])
  return null
}

/**
 * Mapa com a última posição da van (Leaflet + tiles do OpenStreetMap). Carregado só onde é usado (lazy).
 * Tiles que não carregam não escondem nada: hora e avisos ficam fora do mapa.
 */
export default function MapaVan({ latitude, longitude, precisaoM, esmaecido }: MapaVanProps) {
  const [semTiles, setSemTiles] = useState(false)
  const centro: [number, number] = [latitude, longitude]
  return (
    <figure className="relative isolate m-0 h-64 overflow-hidden rounded-lg border sm:h-80" data-desatualizada={esmaecido}>
      <figcaption className="sr-only">Mapa com a última posição da van</figcaption>
      <MapContainer center={centro} zoom={TILES.zoom} maxZoom={TILES.zoomMax} scrollWheelZoom={false} className="h-full w-full">
        <TileLayer
          url={TILES.url}
          attribution={TILES.atribuicao}
          maxZoom={TILES.zoomMax}
          eventHandlers={{ tileerror: () => setSemTiles(true), tileload: () => setSemTiles(false) }}
        />
        <Circle center={centro} radius={precisaoM} pathOptions={{ className: 'fill-primary stroke-primary', fillOpacity: 0.12, opacity: 0.4, weight: 1 }} />
        <CircleMarker
          center={centro}
          radius={10}
          pathOptions={{ className: 'fill-primary stroke-background', fillOpacity: esmaecido ? 0.35 : 0.95, opacity: esmaecido ? 0.5 : 1, weight: 3 }}
        />
        <Seguir latitude={latitude} longitude={longitude} />
      </MapContainer>
      {semTiles && (
        <p className="absolute inset-x-2 top-2 z-[1000] rounded-md bg-background/90 px-2 py-1 text-center text-xs text-muted-foreground">
          Mapa indisponível. A posição continua sendo atualizada.
        </p>
      )}
    </figure>
  )
}
