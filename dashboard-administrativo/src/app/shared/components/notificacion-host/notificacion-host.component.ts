import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NotificacionService } from '../../../core/services/notificacion.service';

@Component({
  selector: 'app-notificacion-host',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './notificacion-host.component.html',
  styleUrl: './notificacion-host.component.css'
})
export class NotificacionHostComponent {
  private readonly notificacionService = inject(NotificacionService);

  readonly toasts = this.notificacionService.toasts;
  readonly modal = this.notificacionService.modalConfirmacion;

  cerrarToast(id: string): void {
    this.notificacionService.removerToast(id);
  }

  confirmar(): void {
    this.notificacionService.responderConfirmacion(true);
  }

  cancelar(): void {
    this.notificacionService.responderConfirmacion(false);
  }

  obtenerClaseEstado(estado?: string): string {
    const e = (estado || '').toLowerCase().trim();
    if (e.includes('finalizado') || e.includes('resuelto')) return 'badge-finalizado';
    if (e.includes('revision')) return 'badge-revision';
    if (e.includes('iniciado') || e.includes('proceso')) return 'badge-iniciado';
    return 'badge-no-iniciado';
  }
}
