export type GuestStatus = 'PENDENTE' | 'CONFIRMADO' | 'RECUSADO';

export interface RsvpResponse {
  nomeCompleto: string;
  codigoConvite: string;
  statusConvite: GuestStatus;
  email: string;
  telefone: string;
  observacoes: string;
}

export interface RsvpActionRequest {
  codigoConvite: string;
  email: string;
  telefone: string;
  observacoes: string;
}