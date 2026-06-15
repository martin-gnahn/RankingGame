import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';

import { environment } from '../../../environments/environment';
import { RoomApiService } from './room-api.service';

describe('RoomApiService', () => {
  let service: RoomApiService;
  let httpTesting: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [RoomApiService, provideHttpClient(), provideHttpClientTesting()],
    });

    service = TestBed.inject(RoomApiService);
    httpTesting = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpTesting.verify();
  });

  it('should create a room with the expected URL and payload', () => {
    const response = { roomCode: 'ABCD', playerId: '7' };

    service.createRoom({ playerName: 'Marta' }).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ playerName: 'Marta' });

    request.flush(response);
  });

  it('should join a room with an encoded room code and expected payload', () => {
    const response = { roomCode: 'A/B1', playerId: '9' };

    service.joinRoom('A/B1', { playerName: 'Alex' }).subscribe((result) => {
      expect(result).toEqual(response);
    });

    const request = httpTesting.expectOne(`${environment.apiBaseUrl}/rooms/A%2FB1/players`);
    expect(request.request.method).toBe('POST');
    expect(request.request.body).toEqual({ playerName: 'Alex' });

    request.flush(response);
  });
});
