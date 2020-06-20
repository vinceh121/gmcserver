import { TestBed } from '@angular/core/testing';

import { WsService } from './ws.service';

describe('WsService', () => {
  let service: WsService;

  beforeEach(() => {
    TestBed.configureTestingModule({});
    service = TestBed.inject(WsService);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });
});
