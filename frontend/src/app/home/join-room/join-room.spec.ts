import { ComponentFixture, TestBed } from '@angular/core/testing';

import { JoinRoom } from './join-room';

describe('JoinRoom', () => {
  let fixture: ComponentFixture<JoinRoom>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [JoinRoom],
    }).compileComponents();

    fixture = TestBed.createComponent(JoinRoom);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should emit sanitized room code and trimmed player name', () => {
    spyOn(fixture.componentInstance.joinRoomRequested, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const inputs = compiled.querySelectorAll<HTMLInputElement>('input');
    inputs[0].value = 'ab-cd9';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = ' Alex ';
    inputs[1].dispatchEvent(new Event('input'));

    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(inputs[0].value).toBe('ABCD9');
    expect(fixture.componentInstance.joinRoomRequested.emit).toHaveBeenCalledWith({
      roomCode: 'ABCD9',
      playerName: 'Alex',
    });
  });

  it('should reject room codes outside the expected length', () => {
    spyOn(fixture.componentInstance.joinRoomRequested, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const inputs = compiled.querySelectorAll<HTMLInputElement>('input');
    inputs[0].value = 'A1';
    inputs[0].dispatchEvent(new Event('input'));
    inputs[1].value = 'Alex';
    inputs[1].dispatchEvent(new Event('input'));

    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));
    fixture.detectChanges();

    expect(fixture.componentInstance.joinRoomRequested.emit).not.toHaveBeenCalled();
    expect(compiled.querySelector('mat-error')?.textContent).toContain('4-8 Zeichen, A-Z oder 0-9');
  });
});
