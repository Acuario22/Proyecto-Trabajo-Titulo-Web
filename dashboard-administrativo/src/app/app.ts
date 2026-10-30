import { Component, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { NotificacionHostComponent } from './shared/components/notificacion-host/notificacion-host.component';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, NotificacionHostComponent],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly title = signal('dashboard-administrativo');
}

