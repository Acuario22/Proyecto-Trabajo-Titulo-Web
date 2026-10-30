import { Component, EventEmitter, HostListener, Input, OnChanges, OnDestroy, Output, SimpleChanges } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Reporte } from '../../../core/services/reporte.service';

@Component({
  selector: 'app-reporte-detalle-modal',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './reporte-detalle-modal.html',
  styleUrl: './reporte-detalle-modal.css'
})
export class ReporteDetalleModalComponent implements OnChanges, OnDestroy {
  @Input() reporte: Reporte | null = null;
  @Input() visible: boolean = false;
  @Output() cerrar = new EventEmitter<void>();

  ngOnChanges(changes: SimpleChanges): void {
    if (changes['visible']) {
      this.actualizarBloqueoScroll(this.visible);
    }
  }

  ngOnDestroy(): void {
    this.actualizarBloqueoScroll(false);
  }

  onCerrar(): void {
    this.actualizarBloqueoScroll(false);
    this.cerrar.emit();
  }

  @HostListener('window:keydown.escape')
  onEscapeKey(): void {
    if (this.visible) {
      this.onCerrar();
    }
  }

  private actualizarBloqueoScroll(bloquear: boolean): void {
    if (typeof document !== 'undefined') {
      document.body.style.overflow = bloquear ? 'hidden' : '';
    }
  }

  obtenerClaseSentimiento(sentimiento?: string): string {
    const s = (sentimiento || '').toLowerCase().trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (s.includes('enoj') || s.includes('ira') || s.includes('molest')) {
      return 'enojado';
    }
    if (s.includes('frustr')) {
      return 'frustrado';
    }
    if (s.includes('positi') || s.includes('felic') || s.includes('agradec')) {
      return 'positivo';
    }
    return 'neutral';
  }

  obtenerClaseUrgencia(urgencia?: string): string {
    const u = (urgencia || '').toLowerCase().trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    if (u.includes('alta') || u.includes('critica') || u.includes('urgente')) {
      return 'alta';
    }
    if (u.includes('media') || u.includes('moderada')) {
      return 'media';
    }
    return 'baja';
  }

  obtenerClaseRiesgo(riesgo?: string): string {
    const r = (riesgo || '').toUpperCase().trim().normalize('NFD').replace(/[\u0300-\u036f]/g, '');
    switch (r) {
      case 'CRITICO':
        return 'riesgo-critico';
      case 'ALTO':
        return 'riesgo-alto';
      case 'MEDIO':
        return 'riesgo-medio';
      case 'BAJO':
        return 'riesgo-bajo';
      default:
        return 'riesgo-ninguno';
    }
  }
}
