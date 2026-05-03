import { Component, ChangeDetectorRef } from '@angular/core';
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
  selector: 'app-signin',
  imports: [ReactiveFormsModule, RouterLink, CommonModule],
  templateUrl: './signin.html',
  styleUrl: './signin.css',
})
export class Signin {
  signinForm: FormGroup;
  errorMessage = '';
  isLoading = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private cdr: ChangeDetectorRef
  ) {
    this.signinForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required]],
    });
  }

  onSubmit(): void {
    if (this.signinForm.invalid) {
      this.signinForm.markAllAsTouched();
      return;
    }

    this.isLoading = true;
    this.errorMessage = '';
    this.signinForm.disable();

    this.authService
  .login(this.signinForm.value)
  .pipe(
    finalize(() => {
      this.isLoading = false;
      this.signinForm.enable();
      this.cdr.detectChanges();
    })
  )
  .subscribe({
    next: () => {
      this.authService.getProfile().subscribe({
        next: () => {
          (this.authService as any).loggedIn.next(true);
          this.router.navigate(['/home']);
        },
        error: () => {
          this.errorMessage = 'Login succeeded, but failed to load profile.';
          this.cdr.detectChanges();
        },
      });
    },
    error: (err) => {
      this.errorMessage =
        err?.error?.message ||
        err?.message ||
        'Invalid email or password.';

      this.cdr.detectChanges();
    },
  });
  }
    goToSignup(event: Event): void {
    event.preventDefault();

    this.isLoading = false;
    this.errorMessage = '';

    this.router.navigate(['/signup']);
  }

}