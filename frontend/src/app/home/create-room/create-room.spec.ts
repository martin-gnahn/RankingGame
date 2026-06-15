import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CreateRoom } from './create-room';

describe('CreateRoom', () => {
  let fixture: ComponentFixture<CreateRoom>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CreateRoom],
    }).compileComponents();

    fixture = TestBed.createComponent(CreateRoom);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(fixture.componentInstance).toBeTruthy();
  });

  it('should emit the trimmed player name', () => {
    spyOn(fixture.componentInstance.createRoomRequested, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector<HTMLInputElement>('input[formControlName="playerName"]');
    input!.value = ' Marta ';
    input!.dispatchEvent(new Event('input'));

    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(fixture.componentInstance.createRoomRequested.emit).toHaveBeenCalledWith({
      playerName: 'Marta',
    });
  });

  it('should reject a whitespace-only player name', () => {
    spyOn(fixture.componentInstance.createRoomRequested, 'emit');

    const compiled = fixture.nativeElement as HTMLElement;
    const input = compiled.querySelector<HTMLInputElement>('input[formControlName="playerName"]');
    input!.value = '   ';
    input!.dispatchEvent(new Event('input'));

    compiled.querySelector('form')!.dispatchEvent(new Event('submit'));

    expect(fixture.componentInstance.createRoomRequested.emit).not.toHaveBeenCalled();
  });
});
