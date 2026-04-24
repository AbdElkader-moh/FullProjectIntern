import { Component, OnInit, ElementRef, ViewChild } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserResponse } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, RouterLink],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  user: UserResponse | null = null;
  isLoading = true;
  errorMessage = '';
  isUploadingPhoto = false;
  showPassword = false;

  constructor(
    private authService: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    if (this.authService.currentUser) {
      this.user = this.authService.currentUser;
      this.isLoading = false;
      return;
    }

    this.authService.getProfile().subscribe({
      next: (user) => {
        this.user = user;
        this.isLoading = false;
      },
      error: () => {
        this.errorMessage = 'Failed to load profile.';
        this.isLoading = false;
      },
    });
  }

  triggerFileUpload(): void {
    this.fileInput.nativeElement.click();
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file = input.files?.[0];
    if (!file) return;

    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result as string;
      this.isUploadingPhoto = true;

      this.authService.updateProfilePicture(base64).subscribe({
        next: (updatedUser) => {
          this.user = updatedUser;
          this.isUploadingPhoto = false;
        },
        error: () => {
          this.errorMessage = 'Failed to update profile picture.';
          this.isUploadingPhoto = false;
        },
      });
    };
    reader.readAsDataURL(file);

    // reset so the same file can be re-selected
    input.value = '';
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }

  getInitials(): string {
    if (!this.user) return '?';
    const f = this.user.firstName?.charAt(0) || '';
    const l = this.user.lastName?.charAt(0) || '';
    return (f + l).toUpperCase();
  }
}