import { Component, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserResponse } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  user: UserResponse | null = null;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Guard already called getProfile() — use the cached user synchronously
    if (this.authService.currentUser) {
      this.user = this.authService.currentUser;
      return;
    }

    // Fallback: fetch if cache is empty (e.g. hard refresh)
    this.authService.getProfile().subscribe({
      next: (user) => (this.user = user),
      error: () => this.router.navigate(['/signin']),
    });
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
