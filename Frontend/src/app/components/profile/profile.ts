import { Component, OnInit, ElementRef, ViewChild, ChangeDetectorRef } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService, UserResponse } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

@Component({
  selector: 'app-profile',
  imports: [CommonModule, RouterLink, FormsModule],
  templateUrl: './profile.html',
  styleUrl: './profile.css',
})
export class Profile implements OnInit {
  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  user: UserResponse | null = null;
  isLoading = true;
  errorMessage = '';
  isUploadingPhoto = false;

  // Password change fields
  oldPassword = '';
  newPassword = '';
  confirmPassword = '';
  isUpdatingPassword = false;
  passwordMessage = '';
  passwordError = '';

  // Visibility toggles
  isChangePasswordVisible = false;

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

    this.isUploadingPhoto = true;
    this.errorMessage = '';
    this.cdr.detectChanges();

    this.authService.updateProfilePicture(file).subscribe({
      next: (updatedUser) => {
        this.user = updatedUser;
        this.isUploadingPhoto = false;
        this.cdr.detectChanges();
      },
      error: () => {
        this.errorMessage = 'Failed to update profile picture.';
        this.isUploadingPhoto = false;
        this.cdr.detectChanges();
      },
    });

    input.value = '';
  }


  logout(): void {
    this.authService.logout().subscribe({
      next: () => this.router.navigate(['/signin']),
      error: () => this.router.navigate(['/signin']),
    });
  }

changePassword(): void {
  if (!this.oldPassword || !this.newPassword || !this.confirmPassword) {
    this.passwordError = 'All password fields are required.';
    this.passwordMessage = '';
    this.cdr.detectChanges();
    return;
  }

  if (this.newPassword !== this.confirmPassword) {
    this.passwordError = 'New passwords do not match.';
    this.passwordMessage = '';
    this.cdr.detectChanges();
    return;
  }

  this.isUpdatingPassword = true;
  this.passwordError = '';
  this.passwordMessage = '';
  this.cdr.detectChanges();

  this.authService
    .changePassword({
      oldPassword: this.oldPassword,
      newPassword: this.newPassword,
    })
    .subscribe({
      next: (res) => {
        this.passwordMessage =
          res?.message || 'Password updated successfully!';

        this.oldPassword = '';
        this.newPassword = '';
        this.confirmPassword = '';
        this.isUpdatingPassword = false;

        this.cdr.detectChanges();
      },
      error: (err) => {
        this.passwordError =
          err?.error?.message ||
          'Failed to update password.';

        this.passwordMessage = '';
        this.isUpdatingPassword = false;

        this.cdr.detectChanges();
      },
    });
}

  toggleChangePassword(): void {
    this.isChangePasswordVisible = !this.isChangePasswordVisible;
    this.cdr.detectChanges();
  }

  getInitials(): string {
    if (!this.user) return '?';
    const f = this.user.firstName?.charAt(0) || '';
    const l = this.user.lastName?.charAt(0) || '';
    return (f + l).toUpperCase();
  }
}
