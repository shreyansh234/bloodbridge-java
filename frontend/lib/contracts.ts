import { z } from "zod";
export const BLOOD_GROUPS = ["A+", "A-", "B+", "B-", "AB+", "AB-", "O+", "O-"] as const;
export const profileSchema = z.object({
  name: z.string().trim().min(2, "Enter your full name.").max(80),
  bloodGroup: z.enum(BLOOD_GROUPS),
  city: z.string().trim().min(2, "Enter your city.").max(80),
  state: z.string().trim().min(2, "Enter your state.").max(80),
  area: z.string().trim().max(80),
  phone: z.string().trim().transform(v => v.replace(/[\s()-]/g, "").replace(/^\+91/, "")).pipe(z.string().regex(/^[6-9]\d{9}$/, "Enter a valid 10-digit Indian mobile number.")),
  available: z.boolean(),
  consent: z.literal(true, { errorMap: () => ({ message: "Please agree to share your donor details." }) }),
});
export type ProfileInput = z.infer<typeof profileSchema>;
export type Donor = { id: string; name: string; bloodGroup: string; state?:string; city: string; area: string; available: boolean; updatedAt: number; sample?: boolean };
export type Profile = Donor & { phone: string; email?:string; consent: boolean };
export type Session = { mode: "external" | "java"; user: { id: string; name: string; email: string; phone?:string; emailVerified?:boolean } | null; profile: Profile | null; csrf?: string; authReady?: boolean; adminReady?:boolean; isAdmin?: boolean };
export type SearchResult = { donors: Donor[]; total: number; page: number; pages: number; stats: { registered: number; available: number; cities: number } };
export const POINTS_PER_DONATION=500;
export const DEMO_PAISE_PER_POINT=25;
export const SAVIOUR_DONATIONS=10;
export const donationSchema=z.object({donationDate:z.string().regex(/^\d{4}-\d{2}-\d{2}$/),center:z.string().trim().min(3,"Enter the blood centre name.").max(120),reference:z.string().trim().min(3,"Enter the donation receipt/reference number.").max(100)});
export const familySchema=z.object({relationship:z.enum(["Self","Parent","Spouse","Child","Sibling","Other family"]),hospital:z.string().trim().min(3).max(120),city:z.string().trim().min(2).max(80),bloodGroup:z.enum(BLOOD_GROUPS),phone:profileSchema.shape.phone,consent:z.literal(true)});
export type DonationRecord={id:string;donationDate:string;center:string;reference:string;status:"pending"|"verified"|"rejected";reviewNote:string;createdAt:number};
export type FamilyRequest={id:string;relationship:string;patientName?:string;email?:string;requesterName?:string;state?:string;emailStatus?:string;hospital:string;city:string;bloodGroup:string;phone:string;status:string;note:string;createdAt:number};
export type RewardsData={verifiedDonations:number;points:number;saviour:boolean;donations:DonationRecord[];requests:FamilyRequest[];familyUsesRemaining?:number;emailReady?:boolean;cashRedemptionEnabled:false;assistanceEnabled:boolean;isAdmin:boolean};
export type BloodMatch={id:string;role:"donor"|"receiver";patientName:string;hospital:string;state:string;city:string;bloodGroup:string;note:string;donorApproved:boolean;receiverApproved:boolean;status:"pending"|"accepted"|"received"|"declined";receivedAt:number|null;createdAt:number};
export type ApiClient=(path:string,options?:RequestInit)=>Promise<any>;
export type InboxThread={id:string;kind:"family"|"donation"|"profile"|"message";subject:string;unread:boolean;createdAt:number;updatedAt:number;draftText?:string;draftRevision?:number;sentRevision?:number};
export type InboxMessage={id:string;role:"system"|"member"|"admin";body:string;createdAt:number};
export type InboxDetail={thread:InboxThread;messages:InboxMessage[];page:number;pages:number};
