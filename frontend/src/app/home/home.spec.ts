import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';

import { RoomApiService } from '../core/api/room-api.service';
import { Home } from './home';

describe('Home', () => {
  let fixture: ComponentFixture<Home>;
  let roomApi: jasmine.SpyObj<RoomApiService>;
  let router: jasmine.SpyObj<Router>;

  beforeEach(async () => {
    roomApi = jasmine.createSpyObj<RoomApiService>('RoomApiService', ['createRoom', 'joinRoom']);
    router = jasmine.createSpyObj<Router>('Router', ['navigate']);

    await TestBed.configureTestingModule({
      imports: [Home],
      providers: [
        { provide: RoomApiService, useValue: roomApi },
        { provide: Router, useValue: router },
      ],
    }).compileComponents();

    fixture = TestBed.createComponent(Home);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should show the start page title', () => {
    const compiled = fixture.nativeElement as HTMLElement;
    expect(compiled.querySelector('h1')?.textContent).toContain('Ranking Game');
  });

  it('should create a room and route to the lobby', () => {
    roomApi.createRoom.and.returnValue(of({ roomCode: 'ABCD', playerId: '7' }));

    const compiled = fixture.nativeElement as HTMLElement;
    const nameInput = compiled.querySelector<HTMLInputElement>('input[formControlName="playerName"]');
    nameInput!.value = 'Marta';
    nameInput!.dispatchEvent(new Event('input'));

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));

    expect(roomApi.createRoom).toHaveBeenCalledWith({ playerName: 'Marta' });
    expect(router.navigate).toHaveBeenCalledWith(['/lobby', 'ABCD'], {
      queryParams: { playerId: '7', role: 'host' },
    });
  });

  it('should join a room and route to the lobby', () => {
    roomApi.joinRoom.and.returnValue(of({ roomCode: 'ABCD9', playerId: '9' }));

    const compiled = fixture.nativeElement as HTMLElement;
    const inputs = compiled.querySelectorAll<HTMLInputElement>('input');
    inputs[1].value = 'ABCD9';
    inputs[1].dispatchEvent(new Event('input'));
    inputs[2].value = 'Alex';
    inputs[2].dispatchEvent(new Event('input'));

    fixture.nativeElement.querySelectorAll('form')[1].dispatchEvent(new Event('submit'));

    expect(roomApi.joinRoom).toHaveBeenCalledWith('ABCD9', { playerName: 'Alex' });
    expect(router.navigate).toHaveBeenCalledWith(['/lobby', 'ABCD9'], {
      queryParams: { playerId: '9', role: 'player' },
    });
  });

  it('should show an error when a room action fails', () => {
    roomApi.createRoom.and.returnValue(throwError(() => new Error('Nope')));

    const compiled = fixture.nativeElement as HTMLElement;
    const nameInput = compiled.querySelector<HTMLInputElement>('input[formControlName="playerName"]');
    nameInput!.value = 'Marta';
    nameInput!.dispatchEvent(new Event('input'));

    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(compiled.querySelector('[role="alert"]')?.textContent).toContain(
      'Die Raumaktion konnte nicht abgeschlossen werden.',
    );
  });
});
