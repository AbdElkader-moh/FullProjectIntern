import { Component, OnInit, ElementRef, ViewChild, ChangeDetectorRef } from '@angular/core';
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
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    if (this.authService.currentUser) {
      this.user = { ...this.authService.currentUser };
      this.isLoading = false;
      this.cdr.detectChanges();
      return;
    }

    this.authService.getProfile().subscribe({
      next: (user) => {
        this.user = { ...user };
        this.isLoading = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to load profile.';
        this.isLoading = false;
        this.cdr.detectChanges();
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
      this.errorMessage = '';
      this.cdr.detectChanges();

this.authService.updateProfilePicture(base64).subscribe({
  next: (updatedUser) => {
    const userWithPicture = {
      ...updatedUser,
      profilePicture: updatedUser.profilePicture || base64,
    };

    this.user = userWithPicture;
    (this.authService as any)._currentUser = userWithPicture;

    this.isUploadingPhoto = false;
    this.cdr.detectChanges();
  },
  error: () => {
    this.errorMessage = 'Failed to update profile picture.';
    this.isUploadingPhoto = false;
    this.cdr.detectChanges();
  },
});
    };

    reader.onerror = () => {
      this.errorMessage = 'Failed to read image.';
      this.isUploadingPhoto = false;
      this.cdr.detectChanges();
    };

    reader.readAsDataURL(file);
    input.value = '';
  }

  togglePassword(): void {
    this.showPassword = !this.showPassword;
    this.cdr.detectChanges();
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