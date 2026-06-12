import { Component } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatDividerModule } from '@angular/material/divider';

@Component({
  selector: 'app-home',
  imports: [MatButtonModule, MatCardModule, MatDividerModule],
  templateUrl: './home.html',
  styleUrl: './home.scss'
})
export class Home {}
