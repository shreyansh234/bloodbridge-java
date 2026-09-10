import { z } from "zod";
export const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"] as const;
export const profileSchema = z.object({
  name: z.string().trim().min(2, "Enter your full name.").max(80),
  bloodGroup: z.enum(BLOOD_GROUPS),
  city: z.string().trim().min(2, "Enter your city.").max(80),
  area: z.string().trim().max(80),
  phone: z.string().trim().transform(v => v.replace(/[\s()-]/g, "").replace(/^\+91/, "")).pipe(z.string().regex(/^[6-9]\d{9}$/, "Enter a valid 10-digit Indian mobile number.")),
  available: z.boolean(),
  consent: z.literal(true, { errorMap: () => ({ message: "Please agree to share your donor details." }) }),
});
export type ProfileInput = z.infer<typeof profileSchema>;
export type Donor = { id: string; name: string; bloodGroup: string; city: string; area: string; available: boolean; updatedAt: number; sample?: boolean };
export type Profile = Donor & { phone: string; consent: boolean };
export type Session = { mode: "platform" | "java"; user: { id: string; name: string; email: string } | null; profile: Profile | null; csrf?: string; signInUrl?: string; signOutUrl?: string };
export type SearchResult = { donors: Donor[]; total: number; page: number; pages: number; stats: { registered: number; available: number; cities: number } };
