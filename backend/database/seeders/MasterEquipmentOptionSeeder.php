<?php

namespace Database\Seeders;

use App\Models\MasterEquipmentOption;
use Illuminate\Database\Seeder;

class MasterEquipmentOptionSeeder extends Seeder
{
    public function run(): void
    {
        $options = [
            // === JENIS_ANTENNA ===
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - CBand 1,8m', 'sort_order' => 1],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - CBand 2,4m', 'sort_order' => 2],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - KaBand 0,74m', 'sort_order' => 3],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - KaBand 0,97m', 'sort_order' => 4],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - KuBand 0,74m', 'sort_order' => 5],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - KuBand 0,97m', 'sort_order' => 6],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - KuBand 1,2m', 'sort_order' => 7],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - KuBand 1,8m', 'sort_order' => 8],
            ['field_key' => 'JENIS_ANTENNA', 'label' => 'ANTENNA - Type Lain', 'sort_order' => 9],

            // === JENIS_MOUNTING ===
            ['field_key' => 'JENIS_MOUNTING', 'label' => 'Baseplate kingpost', 'sort_order' => 1],
            ['field_key' => 'JENIS_MOUNTING', 'label' => 'GroundMount', 'sort_order' => 2],
            ['field_key' => 'JENIS_MOUNTING', 'label' => 'NPRM', 'sort_order' => 3],
            ['field_key' => 'JENIS_MOUNTING', 'label' => 'Wallmount', 'sort_order' => 4],
            ['field_key' => 'JENIS_MOUNTING', 'label' => 'Type lain', 'sort_order' => 5],

            // === TYPE_MODEM ===
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HN', 'sort_order' => 1],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HT2010', 'sort_order' => 2],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HT2300', 'sort_order' => 3],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HT2500', 'sort_order' => 4],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HT2524', 'sort_order' => 5],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HT3300', 'sort_order' => 6],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HX50', 'sort_order' => 7],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HX50L', 'sort_order' => 8],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem HX50M', 'sort_order' => 9],
            ['field_key' => 'TYPE_MODEM', 'label' => 'Modem type lain', 'sort_order' => 10],

            // === PENYEBAB_GANGGUAN ===
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'ANTENNA - Terhalang (Pohon/Tanaman/Bangunan)', 'sort_order' => 1],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'ANTENNA - Pointing Bergeser', 'sort_order' => 2],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'ANTENNA - Rusak / Roboh', 'sort_order' => 3],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'ANTENNA - Feedhorn (Membran rusak)', 'sort_order' => 4],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'BENCANA_ALAM - Banjir / Gempa / Vulkanik', 'sort_order' => 5],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'GANGGUAN_EXTERNAL - Dicurigai perangkat customer (PC / Mesin ATM / LAN ATM) bermasalah', 'sort_order' => 6],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'GANGGUAN_EXTERNAL - KABEL IFL / KABEL Power dipindah atau dicabut pihak lain', 'sort_order' => 7],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'GANGGUAN_EXTERNAL - Kejahatan (Pengrusakan / Pembakaran / Pencurian)', 'sort_order' => 8],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'GANGGUAN_LAIN - Link Intermittent (Belum diketahui penyebabnya)', 'sort_order' => 9],
            ['field_key' => 'PENYEBAB_GANGGUAN', 'label' => 'GANGGUAN_LAIN - Satelit', 'sort_order' => 10],

            // === FOTO_WAJIB_SCM (Checklist foto yang wajib diupload) ===
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Antena Tampak Depan (keseluruhan)', 'sort_order' => 1],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Antena Tampak Belakang (keseluruhan)', 'sort_order' => 2],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto SN Modem', 'sort_order' => 3],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto SN BUC', 'sort_order' => 4],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto SN LNB', 'sort_order' => 5],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Modem UP', 'sort_order' => 6],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Rak/Box (Tampak Keseluruhan ATM)', 'sort_order' => 7],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Plang Lokasi', 'sort_order' => 8],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto LNB + BUC + Feedhorn', 'sort_order' => 9],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Feedhorn', 'sort_order' => 10],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Jalur Kabel Out', 'sort_order' => 11],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Jalur Kabel In', 'sort_order' => 12],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Capture Summary Modem', 'sort_order' => 13],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Test Ping (1000)', 'sort_order' => 14],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Teknisi di Lokasi', 'sort_order' => 15],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Terlihat Dynabolt pada Pedestal', 'sort_order' => 16],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Pengukuran Listrik (PLN dan UPS)', 'sort_order' => 17],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Test Ping (100)', 'sort_order' => 18],
            ['field_key' => 'FOTO_WAJIB_SCM', 'label' => 'Foto Lain-lain (Foto Pelengkap)', 'sort_order' => 99],
        ];

        foreach ($options as $data) {
            MasterEquipmentOption::firstOrCreate(
                ['field_key' => $data['field_key'], 'label' => $data['label']],
                array_merge($data, ['is_active' => true])
            );
        }

        echo "Seeded " . count($options) . " equipment options\n";
    }
}
