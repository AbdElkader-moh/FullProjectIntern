import { Component, OnDestroy } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-signup',
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './signup.html',
  styleUrl: './signup.css',
})
export class Signup implements OnDestroy {
  signupForm: FormGroup;
  errorMessage = '';
  isLoading = false;
  isImageLoading = false;
  imagePreview: string | null = null;
  fileName: string | null = null;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.signupForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      profilePicture: [''],
    });
  }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files[0]) {
      const file = input.files[0];
      this.fileName = file.name;

      // Clean up previous blob URL if it exists
      if (this.imagePreview && this.imagePreview.startsWith('blob:')) {
        URL.revokeObjectURL(this.imagePreview);
      }

      // 1. Use Blob URL for instant preview
      this.imagePreview = URL.createObjectURL(file);
      this.isImageLoading = false;

      // 2. Convert to base64 in background for the backend
      const reader = new FileReader();
      reader.onload = () => {
        this.signupForm.patchValue({ profilePicture: reader.result as string });
      };
      reader.readAsDataURL(file);
    }
  }

  removeImage(): void {
    if (this.imagePreview && this.imagePreview.startsWith('blob:')) {
      URL.revokeObjectURL(this.imagePreview);
    }
    this.imagePreview = null;
    this.fileName = null;
    this.signupForm.patchValue({ profilePicture: '' });
  }

  ngOnDestroy(): void {
    // Final cleanup
    if (this.imagePreview && this.imagePreview.startsWith('blob:')) {
      URL.revokeObjectURL(this.imagePreview);
    }
  }

  isFieldInvalid(fieldName: string): boolean {
    const field = this.signupForm.get(fieldName);
    return !!(field && field.invalid && (field.dirty || field.touched));
  }

  onSubmit(): void {
    if (this.signupForm.invalid) {
      this.signupForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';

    this.authService.signup(this.signupForm.value).subscribe({
      next: () => {
        this.router.navigate(['/signin']);
      },
      error: (err) => {
        this.isLoading = false;
        this.errorMessage =
          err.error?.message || 'Registration failed. Please try again.';
      },
    });
  }
}
