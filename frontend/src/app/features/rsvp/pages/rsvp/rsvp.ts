import { Component, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { RsvpService } from '../../services/rsvp.service';
import { RsvpResponse } from '../../models/rsvp.models';

@Component({
  selector: 'app-rsvp',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './rsvp.html',
  styleUrl: './rsvp.css',
})
export class Rsvp {
  private fb = inject(FormBuilder);
  private rsvpService = inject(RsvpService);

  guestData = signal<RsvpResponse | null>(null);
  isLoading = signal(false);
  errorMessage = signal('');
  successMessage = signal('');

  lookupForm = this.fb.group({
    codigoConvite: ['', [Validators.required, Validators.minLength(3)]]
  });

  actionForm = this.fb.group({
    email: ['', [Validators.email]],
    telefone: ['', [Validators.required]],
    observacoes: ['']
  });

  onLookup() {
    if (this.lookupForm.invalid) return;

    this.isLoading.set(true);
    this.errorMessage.set('');
    
    const codigo = this.lookupForm.value.codigoConvite!;

    this.rsvpService.lookup(codigo).subscribe({
      next: (data) => {
        this.guestData.set(data);
        this.actionForm.patchValue({
          email: data.email,
          telefone: data.telefone,
          observacoes: data.observacoes
        });
        this.isLoading.set(false);
      },
      error: (err) => {
        console.error(err);
        this.errorMessage.set('Convite não encontrado ou erro no servidor.');
        this.isLoading.set(false);
        this.guestData.set(null);
      }
    });
  }

  submitRsvp(isConfirm: boolean) {
    if (this.actionForm.invalid) {
        this.actionForm.markAllAsTouched();
        return;
    }
    
    this.isLoading.set(true);
    const codigo = this.guestData()?.codigoConvite!;
    
    const payload = {
      codigoConvite: codigo,
      ...this.actionForm.getRawValue()
    };

    const onSuccess = () => {
        this.successMessage.set(isConfirm ? 'Presença confirmada! 🎉' : 'Que pena que não poderá ir. 😢');
        this.isLoading.set(false);
        this.guestData.update(g => g ? { ...g, statusConvite: isConfirm ? 'CONFIRMADO' : 'RECUSADO' } : null);
    };

    if (isConfirm) {
      this.rsvpService.confirm(payload as any).subscribe({ next: onSuccess, error: () => this.isLoading.set(false) });
    } else {
      this.rsvpService.decline(payload as any).subscribe({ next: onSuccess, error: () => this.isLoading.set(false) });
    }
  }
}