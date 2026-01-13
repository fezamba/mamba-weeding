import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { RsvpResponse, RsvpActionRequest } from '../models/rsvp.models'; 

@Injectable({
  providedIn: 'root',
})
export class RsvpService {
  private http = inject(HttpClient);
  // FIXME: Criar .env para a URL da API
  private apiUrl = 'http://localhost:8080/api/rsvp'; 

  lookup(codigoConvite: string): Observable<RsvpResponse> {
    return this.http.post<RsvpResponse>(`${this.apiUrl}/lookup`, { codigoConvite });
  }

  confirm(request: RsvpActionRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/confirm`, request);
  }

  decline(request: RsvpActionRequest): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/decline`, request);
  }
}