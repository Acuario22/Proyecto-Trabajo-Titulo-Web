import { Injectable } from '@angular/core';

export interface PanelEstado {
  categoria: string;
  idEstado: number | null;
  pagina: number;
  tamanoPagina: number;
  intervaloRecarga: number;
}

@Injectable({
  providedIn: 'root'
})
export class PanelEstadoService {
  private readonly STORAGE_KEY = 'panel-administrativo-estado';

  private readonly estadoPorDefecto: PanelEstado = {
    categoria: 'Todos',
    idEstado: null,
    pagina: 1,
    tamanoPagina: 10,
    intervaloRecarga: 60
  };

  private estadoActual: PanelEstado;

  constructor() {
    this.estadoActual = this.recuperarEstado();
  }

  get categoria(): string {
    return this.estadoActual.categoria;
  }

  get idEstado(): number | null {
    return this.estadoActual.idEstado;
  }

  get pagina(): number {
    return this.estadoActual.pagina;
  }

  get tamanoPagina(): number {
    return this.estadoActual.tamanoPagina;
  }

  get intervaloRecarga(): number {
    return this.estadoActual.intervaloRecarga;
  }

  get estado(): PanelEstado {
    return { ...this.estadoActual };
  }

  actualizarCategoria(categoria: string): void {
    this.estadoActual.categoria = categoria || 'Todos';
    this.guardarEstado();
  }

  actualizarEstado(idEstado: number | null): void {
    this.estadoActual.idEstado = idEstado;
    this.guardarEstado();
  }

  actualizarPagina(pagina: number): void {
    this.estadoActual.pagina = Math.max(1, pagina || 1);
    this.guardarEstado();
  }

  actualizarTamanoPagina(tamano: number): void {
    this.estadoActual.tamanoPagina = tamano || 10;
    this.guardarEstado();
  }

  actualizarIntervalo(intervalo: number): void {
    this.estadoActual.intervaloRecarga = intervalo;
    this.guardarEstado();
  }

  restablecerFiltros(): void {
    this.estadoActual.categoria = 'Todos';
    this.estadoActual.idEstado = null;
    this.estadoActual.pagina = 1;
    this.guardarEstado();
  }

  reiniciar(): void {
    this.estadoActual = { ...this.estadoPorDefecto };
    if (typeof window !== 'undefined' && window.sessionStorage) {
      try {
        sessionStorage.removeItem(this.STORAGE_KEY);
      } catch {
        // Ignorar restricciones en entornos con storage bloqueado
      }
    }
  }

  private guardarEstado(): void {
    if (typeof window === 'undefined' || !window.sessionStorage) {
      return;
    }
    try {
      sessionStorage.setItem(this.STORAGE_KEY, JSON.stringify(this.estadoActual));
    } catch {
      // Ignorar restricciones en entornos con storage bloqueado
    }
  }

  private recuperarEstado(): PanelEstado {
    if (typeof window === 'undefined' || !window.sessionStorage) {
      return { ...this.estadoPorDefecto };
    }
    try {
      const guardado = sessionStorage.getItem(this.STORAGE_KEY);
      if (guardado) {
        const parsed = JSON.parse(guardado);
        return {
          categoria: typeof parsed.categoria === 'string' && parsed.categoria.trim() !== ''
            ? parsed.categoria
            : this.estadoPorDefecto.categoria,
          idEstado: typeof parsed.idEstado === 'number' && [1, 2, 3, 4].includes(parsed.idEstado)
            ? parsed.idEstado
            : null,
          pagina: typeof parsed.pagina === 'number' && parsed.pagina >= 1
            ? parsed.pagina
            : 1,
          tamanoPagina: [10, 20, 40].includes(parsed.tamanoPagina)
            ? parsed.tamanoPagina
            : 10,
          intervaloRecarga: typeof parsed.intervaloRecarga === 'number'
            ? parsed.intervaloRecarga
            : 60
        };
      }
    } catch {
      // Retornar valores por defecto en caso de formato inválido
    }
    return { ...this.estadoPorDefecto };
  }
}

