import type { Metadata } from "next";
import { PracticeCatalog } from "@/features/courses/practice-catalog";

export const metadata: Metadata = { title: "Thực hành lập trình" };
export default function PracticePage() {
  return <PracticeCatalog />;
}
