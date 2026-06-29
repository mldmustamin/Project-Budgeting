<?php

namespace Database\Seeders;

use App\Models\BudgetItemTemplate;
use App\Models\PaguJobTypeAmount;
use Illuminate\Database\Seeder;
use Illuminate\Support\Str;

class BudgetItemTemplateSeeder extends Seeder
{
    public function run(): void
    {
        $templates = [
            // ===== FIXED_PAGU (10 items — amounts from the pagu table) =====
            ['category_name' => 'Akomodasi Paket (Luar Homebase)', 'category_group' => 'PAKET_LK', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 120000, 'pagu_note' => 'Per hari', 'requires_bill' => false, 'display_order' => 1],
            ['category_name' => 'Akomodasi Hotel (Luar Homebase)', 'category_group' => 'HOTEL_LK', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 200000, 'pagu_note' => 'Per malam. Hotel Papua: 275.000. Boleh lebih dgn bill asli', 'requires_bill' => true, 'bill_note' => 'Bill asli wajib jika melebihi pagu. Tanpa bill = dipotong ke pagu', 'display_order' => 2],
            ['category_name' => 'Akomodasi Hotel (Khusus Papua)', 'category_group' => 'HOTEL_PAPUA', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 275000, 'pagu_note' => 'Per malam, khusus lokasi Papua', 'requires_bill' => true, 'bill_note' => 'Bill asli wajib jika melebihi pagu', 'display_order' => 3],
            ['category_name' => 'Biaya Lain-Voucher', 'category_group' => 'VOUCHER_HP', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 15000, 'pagu_note' => 'Instalasi/Relokasi/PMCM: 15.000. Dismantle/Survey: 5.000', 'requires_bill' => false, 'display_order' => 4],
            ['category_name' => 'Biaya Lain-Buruh', 'category_group' => 'BURUH', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 120000, 'pagu_note' => 'Instalasi/Relokasi. Dikoordinasikan dengan Manager', 'requires_bill' => false, 'display_order' => 5],
            ['category_name' => 'Biaya Lain-Ballast/Pondasi', 'category_group' => 'BALLAST', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => 200000, 'pagu_note' => 'Instalasi/Relokasi. Include transport material', 'requires_bill' => false, 'display_order' => 6],
            ['category_name' => 'Transport Ojek Berangkat', 'category_group' => 'TRANSPORT_OJEK_BERANGKAT', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => null, 'pagu_note' => 'Sesuai Gojek online / bensin 20.000/hari', 'requires_bill' => false, 'display_order' => 7],
            ['category_name' => 'Transport Ojek Pulang', 'category_group' => 'TRANSPORT_OJEK_PULANG', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => null, 'pagu_note' => 'Sesuai Gojek online / bensin 20.000/hari', 'requires_bill' => false, 'display_order' => 8],
            ['category_name' => 'Transport Bensin', 'category_group' => 'TRANSPORT_BENSIN', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => null, 'pagu_note' => '20.000/hari', 'requires_bill' => false, 'display_order' => 9],
            ['category_name' => 'Biaya Lain-Fee Pekerjaan', 'category_group' => 'FEE_PEKERJAAN', 'pagu_type' => 'FIXED_PAGU', 'pagu_amount' => null, 'pagu_note' => 'Instalasi: 40.000, Relokasi: 75.000, PMCM/Dismantle: 15.000, Survey: 0', 'requires_bill' => false, 'display_order' => 10],

            // ===== TICKET (12 items — wajib bukti fisik) =====
            ['category_name' => 'Tiket Pesawat Berangkat', 'category_group' => 'TIKET_PESAWAT_BERANGKAT', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB. Tidak boleh dicoret. Tanpa bukti = Finance tentukan nominal saat rekonsiliasi', 'display_order' => 11],
            ['category_name' => 'Tiket Pesawat Pulang', 'category_group' => 'TIKET_PESAWAT_PULANG', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 12],
            ['category_name' => 'Tiket Ferry Berangkat', 'category_group' => 'TIKET_FERRY_BERANGKAT', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 13],
            ['category_name' => 'Tiket Ferry Pulang', 'category_group' => 'TIKET_FERRY_PULANG', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 14],
            ['category_name' => 'Tiket Bis Berangkat', 'category_group' => 'TIKET_BIS_BERANGKAT', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 15],
            ['category_name' => 'Tiket Bis Pulang', 'category_group' => 'TIKET_BIS_PULANG', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 16],
            ['category_name' => 'Tiket Travel Berangkat', 'category_group' => 'TIKET_TRAVEL_BERANGKAT', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 17],
            ['category_name' => 'Tiket Travel Pulang', 'category_group' => 'TIKET_TRAVEL_PULANG', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 18],
            ['category_name' => 'Tiket Kereta Berangkat', 'category_group' => 'TIKET_KERETA_BERANGKAT', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 19],
            ['category_name' => 'Tiket Kereta Pulang', 'category_group' => 'TIKET_KERETA_PULANG', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 20],
            ['category_name' => 'Tiket Lainnya', 'category_group' => 'TIKET_LAINNYA', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Tiket resmi WAJIB', 'display_order' => 21],
            ['category_name' => 'Transport Lainnya', 'category_group' => 'TRANSPORT_LAINNYA', 'pagu_type' => 'TICKET', 'pagu_amount' => null, 'requires_bill' => true, 'bill_note' => 'Travel/Bus/Kapal', 'display_order' => 22],

            // ===== MANAGER_APPROVAL (13 items — OWNER determines amount) =====
            ['category_name' => 'Transport Taksi Berangkat', 'category_group' => 'TRANSPORT_TAKSI_BERANGKAT', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 23],
            ['category_name' => 'Transport Taksi Pulang', 'category_group' => 'TRANSPORT_TAKSI_PULANG', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 24],
            ['category_name' => 'Transport Ojek Beli Material', 'category_group' => 'TRANSPORT_OJEK_BELI_MATERIAL', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 25],
            ['category_name' => 'Akomodasi Uang Makan (Homebase)', 'category_group' => 'AKOMODASI_UANG_MAKAN', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 26],
            ['category_name' => 'Akomodasi Lainnya', 'category_group' => 'AKOMODASI_LAINNYA', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 27],
            ['category_name' => 'Biaya Lain-Material', 'category_group' => 'BIAYA_LAIN_MATERIAL', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 28],
            ['category_name' => 'Biaya Lain-Lifting', 'category_group' => 'BIAYA_LAIN_LIFTING', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 29],
            ['category_name' => 'Biaya Lain-Tarik Kabel', 'category_group' => 'BIAYA_LAIN_TARIK_KABEL', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 30],
            ['category_name' => 'Biaya Lain-Tebang Pohon', 'category_group' => 'BIAYA_LAIN_TEBANG_POHON', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 31],
            ['category_name' => 'Biaya Lainnya', 'category_group' => 'BIAYA_LAINNYA', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 32],
            ['category_name' => 'Pengembalian Dana', 'category_group' => 'PENGEMBALIAN_DANA', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 33],
            ['category_name' => 'Biaya Mounting (Modifikasi/Baru)', 'category_group' => 'BIAYA_MOUNTING', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 34],
            ['category_name' => 'Jasa Subcont', 'category_group' => 'JASA_SUBCONT', 'pagu_type' => 'MANAGER_APPROVAL', 'pagu_amount' => null, 'requires_bill' => false, 'display_order' => 35],
        ];

        foreach ($templates as $data) {
            BudgetItemTemplate::firstOrCreate(
                ['category_group' => $data['category_group']],
                array_merge(['uuid' => (string) Str::uuid()], $data)
            );
        }

        echo "Seeded " . count($templates) . " budget item templates\n";

        // Seed per-job-type amounts
        $this->seedPaguAmounts();
    }

    private function seedPaguAmounts(): void
    {
        // Mapping: category_group → [job_type => amount]
        // Amounts from the official pagu table. null = not applicable.
        $amounts = [
            'PAKET_LK' => [
                'INSTALASI' => 120000, 'RELOKASI' => 120000, 'PMCM' => 120000,
                'DISMANTLE' => 120000, 'SURVEY' => 120000,
            ],
            'HOTEL_LK' => [
                'INSTALASI' => 200000, 'RELOKASI' => 200000, 'PMCM' => 200000,
                'DISMANTLE' => 200000, 'SURVEY' => 200000,
            ],
            'HOTEL_PAPUA' => [
                'INSTALASI' => 275000, 'RELOKASI' => 275000, 'PMCM' => 275000,
                'DISMANTLE' => 275000, 'SURVEY' => 275000,
            ],
            'VOUCHER_HP' => [
                'INSTALASI' => 15000, 'RELOKASI' => 15000, 'PMCM' => 15000,
                'DISMANTLE' => 5000, 'SURVEY' => 5000,
            ],
            'BURUH' => [
                'INSTALASI' => 120000, 'RELOKASI' => 120000,
                'PMCM' => null, 'DISMANTLE' => null, 'SURVEY' => null,
            ],
            'BALLAST' => [
                'INSTALASI' => 200000, 'RELOKASI' => 200000,
                'PMCM' => null, 'DISMANTLE' => null, 'SURVEY' => null,
            ],
            'FEE_PEKERJAAN' => [
                'INSTALASI' => 40000, 'RELOKASI' => 75000, 'PMCM' => 15000,
                'DISMANTLE' => 15000, 'SURVEY' => null,
            ],
        ];

        $count = 0;
        foreach ($amounts as $categoryGroup => $jobTypeAmounts) {
            $template = BudgetItemTemplate::where('category_group', $categoryGroup)->first();
            if (!$template) continue;

            foreach ($jobTypeAmounts as $jobType => $amount) {
                PaguJobTypeAmount::firstOrCreate(
                    ['template_id' => $template->id, 'job_type' => $jobType],
                    ['amount' => $amount]
                );
                $count++;
            }
        }

        echo "Seeded {$count} pagu job type amounts\n";
    }
}
