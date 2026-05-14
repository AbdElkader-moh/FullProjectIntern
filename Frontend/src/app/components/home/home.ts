import { Component, OnInit, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserResponse } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-home',
  imports: [CommonModule, RouterLink],
  templateUrl: './home.html',
  styleUrl: './home.css',
})
export class Home implements OnInit {
  user: UserResponse | null = null;
  unreadCount = 0;

  constructor(
    private authService: AuthService,
    private notificationService: NotificationService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

 ngOnInit(): void {
  if (this.authService.currentUser) {
    this.user = this.authService.currentUser;
  } else {
    this.authService.getProfile().subscribe({
      next: (user) => {
        this.user = user;
        this.cdr.detectChanges();
      },
      error: () => this.router.navigate(['/signin']),
    });
  }

  this.notificationService.unreadCount$.subscribe((count: number) => {
    this.unreadCount = count;
    this.cdr.detectChanges();
  });
}


  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }
}
