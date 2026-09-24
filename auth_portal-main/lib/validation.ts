import * as z from "zod"
import {passwordRules} from "./password"

export const firstNameSchema = z
    .string()
    .min(1, "First name is required")
    .max(25, "That's a bit long for a first name")
    .regex(/^[\p{L} '-]+$/u, "There are characters that we don't support")
    .transform((v) => v.trim().toLowerCase())


export const lastNameSchema = z
    .string()
    .min(1, "Last name is required")
    .max(25, "That's a bit long for a last name")
    .regex(/^[\p{L} '-]+$/u, "There are characters that we don't support")
    .transform((v) => v.trim().toLowerCase())


export const emailSchema = z
    .string()
    .min(1, "Email is required")
    .max(50, "That's a bit long for an email")
    .email("Enter a valid email address")
    .transform((v) => v.trim().toLowerCase())


export const passwordSchema = z
    .string()
    .min(1, "Password is required")
    .refine((v) => passwordRules.every((r) => r.test(v)), {
        message: "Password does not meet all the requirements below",
    })

export const confirmPasswordSchema = z
    .string()
    .min(1, "Please repeat the password")

export const verificationCodeSchema = z
    .string()
    .length(6, "The code has 6 characters")
    .transform((v) => v.trim())

export const numericVerificationCodeSchema = z
    .string()
    .regex(/^\d+$/, "Only numbers allowed")
    .length(6, "The code has 6 digits")
    .transform((v) => v.trim())


export const recoveryCodeSchema = z
    .string()
    .min(1, "Recovery code is required")
    .max(25, "That's a bit long for a recovery code")
    .transform((v) => v.trim())

