import { Injectable, signal } from '@angular/core';

export type TipoNotificacion = 'exito' | 'error' | 'advertencia' | 'info';

export interface ToastNotificacion {
  id: string;
  tipo: TipoNotificacion;
  titulo: string;
  mensaje: string;
  duracion: number;
}

export interface ConfirmModalConfig {
  titulo: string;
  mensaje: string;
  estadoActual?: string;
  estadoNuevo?: string;
  textoConfirmar?: string;
  textoCancelar?: string;
  tipo?: 'primario' | 'exito' | 'peligro';
}

export interface ConfirmModalState extends ConfirmModalConfig {
  visible: boolean;
  resolve?: (resultado: boolean) => void;
}

@Injectable({
  providedIn: 'root'
})
export class NotificacionService {
  // Lista reactiva de toasts activos
  readonly toasts = signal<ToastNotificacion[]>([]);

  // Estado reactivo del modal de confirmación
  readonly modalConfirmacion = signal<ConfirmModalState>({
    visible: false,
    titulo: '',
    mensaje: ''
  });

  private contadorId = 0;

  /**
   * Muestra un Toast de éxito (verde satisfacción)
   */
  exito(titulo: string, mensaje: string, duracion: number = 4000): void {
    this.agregarToast('exito', titulo, mensaje, duracion);
  }

  /**
   * Muestra un Toast de error/peligro (rojo coral)
   */
  error(titulo: string, mensaje: string, duracion: number = 4000): void {
    this.agregarToast('error', titulo, mensaje, duracion);
  }

  /**
   * Muestra un Toast de advertencia (ámbar aviso)
   */
  advertencia(titulo: string, mensaje: string, duracion: number = 4000): void {
    this.agregarToast('advertencia', titulo, mensaje, duracion);
  }

  /**
   * Muestra un Toast de información (azul soporte)
   */
  info(titulo: string, mensaje: string, duracion: number = 4000): void {
    this.agregarToast('info', titulo, mensaje, duracion);
  }

  /**
   * Cierra manualmente un Toast por su ID
   */
  removerToast(id: string): void {
    this.toasts.update(lista => lista.filter(t => t.id !== id));
  }

  /**
   * Abre un modal de confirmación centrado y retorna una Promesa booleana:
   * - true: si el usuario confirma
   * - false: si cancela o cierra
   */
  confirmar(config: ConfirmModalConfig): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.modalConfirmacion.set({
        ...config,
        textoConfirmar: config.textoConfirmar || 'Confirmar',
        textoCancelar: config.textoCancelar || 'Volver y revisar',
        tipo: config.tipo || 'primario',
        visible: true,
        resolve
      });
    });
  }

  /**
   * Resuelve y cierra el modal de confirmación activo
   */
  responderConfirmacion(resultado: boolean): void {
    const estado = this.modalConfirmacion();
    if (estado.resolve) {
      estado.resolve(resultado);
    }
    this.modalConfirmacion.set({
      visible: false,
      titulo: '',
      mensaje: ''
    });
  }

  private agregarToast(tipo: TipoNotificacion, titulo: string, mensaje: string, duracion: number): void {
    const id = `toast-${Date.now()}-${++this.contadorId}`;
    const nuevoToast: ToastNotificacion = {
      id,
      tipo,
      titulo,
      mensaje,
      duracion
    };

    this.toasts.update(lista => [...lista, nuevoToast]);

    if (duracion > 0) {
      setTimeout(() => {
        this.removerToast(id);
      }, duracion);
    }
  }
}
