import { TestBed } from '@angular/core/testing';

import { Rsvp } from './rsvp';

describe('Rsvp', () => {
  let service: Rsvp;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(Rsvp);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
