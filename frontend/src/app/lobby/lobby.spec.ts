import { HttpErrorResponse } from '@angular/common/http';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { provideRouter } from '@angular/router';
import { BehaviorSubject, Subject, of, throwError } from 'rxjs';

import { RoomApiService } from '../core/api/room-api.service';
import { RoomResponse } from '../core/api/room.models';
import { Lobby } from './lobby';

describe('Lobby', () => {
  let fixture: ComponentFixture<Lobby>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let paramMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let queryParamMap: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  const roomResponse: RoomResponse = {
    roomId: 'room-1',
    roomCode: 'ABCD12',
    status: 'LOBBY',
    players: [
      {
        playerId: 'host-1',
        nickname: 'Marta',
        host: true,
        connectionStatus: 'CONNECTED',
      },
      {
        playerId: 'player-2',
        nickname: 'Alex',
        host: false,
        connectionStatus: 'DISCONNECTED',
      },
    ],
  };

  beforeEach(async () => {
    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', ['getRoom']);
    paramMap = new BehaviorSubject(convertToParamMap({ roomCode: 'ABCD12' }));
    queryParamMap = new BehaviorSubject(convertToParamMap({ playerId: 'host-1', role: 'host' }));

    await TestBed.configureTestingModule({
      imports: [Lobby],
      providers: [
        provideRouter([]),
        { provide: RoomApiService, useValue: roomApi },
        {
          provide: ActivatedRoute,
          useValue: {
            paramMap: paramMap.asObservable(),
            queryParamMap: queryParamMap.asObservable(),
          },
        },
      ],
    }).compileComponents();
  });

  function createComponent(): void {
    fixture = TestBed.createComponent(Lobby);
    fixture.detectChanges();
  }

  function textContent(): string {
    return (fixture.nativeElement as HTMLElement).textContent ?? '';
  }

  it('should create', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should load the room from the route room code', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(roomApi.getRoom).toHaveBeenCalledOnceWith('ABCD12');
    expect(textContent()).toContain('ABCD12');
  });

  it('should render players with host and connection status', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    const compiled = fixture.nativeElement as HTMLElement;
    expect(textContent()).toContain('2 Spieler');
    expect(textContent()).toContain('Marta');
    expect(textContent()).toContain('Alex');
    expect(textContent()).toContain('Host');
    expect(textContent()).toContain('Online');
    expect(textContent()).toContain('Getrennt');
    expect(compiled.querySelector('.player-row.current-player')?.textContent).toContain('Marta');
  });

  it('should show the start button only for the current host', () => {
    roomApi.getRoom.and.returnValue(of(roomResponse));

    createComponent();

    expect(textContent()).toContain('Spiel starten');

    queryParamMap.next(convertToParamMap({ playerId: 'player-2', role: 'player' }));
    fixture.detectChanges();

    expect(textContent()).not.toContain('Spiel starten');
    expect(textContent()).toContain('Der Host startet das Spiel.');
  });

  it('should show a loading state while the room request is pending', () => {
    const roomSubject = new Subject<RoomResponse>();
    roomApi.getRoom.and.returnValue(roomSubject.asObservable());

    createComponent();

    expect(textContent()).toContain('Lobby wird geladen');

    roomSubject.next(roomResponse);
    roomSubject.complete();
    fixture.detectChanges();

    expect(textContent()).toContain('Marta');
  });

  it('should show an error state when loading fails', () => {
    roomApi.getRoom.and.returnValue(
      throwError(() => new HttpErrorResponse({ status: 404, error: { message: 'Room not found' } })),
    );

    createComponent();

    expect(textContent()).toContain('Room not found');
    expect(textContent()).toContain('Erneut versuchen');
  });
});
