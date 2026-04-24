import { Component, OnDestroy, ChangeDetectorRef } from '@angular/core';
import {
  FormBuilder,
  FormGroup,
  Validators,
  ReactiveFormsModule,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';
import { CommonModule } from '@angular/common';
import { finalize } from 'rxjs/operators';

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
    private router: Router,
    private cdr: ChangeDetectorRef // 🔥 added
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
    if (!input.files || !input.files[0]) return;

    const file = input.files[0];

    if (file.size > 5 * 1024 * 1024) {
      this.errorMessage = 'Image must be under 5MB.';
      this.cdr.detectChanges(); // 🔥 force update
      return;
    }

    this.errorMessage = '';
    this.fileName = file.name;

    if (this.imagePreview && this.imagePreview.startsWith('blob:')) {
      URL.revokeObjectURL(this.imagePreview);
    }

    this.imagePreview = URL.createObjectURL(file);
    this.isImageLoading = true;
    this.cdr.detectChanges(); // 🔥 show loading immediately

    const reader = new FileReader();

    reader.onload = () => {
      this.signupForm.patchValue({
        profilePicture: reader.result as string,
      });

      this.isImageLoading = false;
      this.cdr.detectChanges(); // 🔥 update UI after upload
    };

    reader.onerror = () => {
      this.errorMessage = 'Failed to read image. Please try again.';
      this.removeImage();
      this.isImageLoading = false;
      this.cdr.detectChanges(); // 🔥 show error immediately
    };

    reader.readAsDataURL(file);
  }

  removeImage(): void {
    if (this.imagePreview && this.imagePreview.startsWith('blob:')) {
      URL.revokeObjectURL(this.imagePreview);
    }

    this.imagePreview = null;
    this.fileName = null;
    this.signupForm.patchValue({ profilePicture: '' });

    this.cdr.detectChanges(); // 🔥 update UI
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

    if (this.isImageLoading) return;

    this.isLoading = true;
    this.errorMessage = '';
    this.cdr.detectChanges(); // 🔥 show loading instantly

    this.authService
      .signup(this.signupForm.value)
      .pipe(
        finalize(() => {
          this.isLoading = false;
          this.cdr.detectChanges(); // 🔥 always stop spinner
        })
      )
      .subscribe({
        next: () => {
          this.router.navigate(['/signin']);
        },
        error: (err) => {
          this.errorMessage =
            err?.error?.message ||
            'Registration failed. Please try again.';
          this.cdr.detectChanges(); // 🔥 show error immediately
        },
      });
  }

  ngOnDestroy(): void {
    if (this.imagePreview && this.imagePreview.startsWith('blob:')) {
      URL.revokeObjectURL(this.imagePreview);
    }
  }
}