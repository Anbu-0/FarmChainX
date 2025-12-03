// src/app/components/qr-scanner/qr-scanner.ts
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { ZXingScannerModule } from '@zxing/ngx-scanner';
import { BarcodeFormat } from '@zxing/library';

@Component({
  selector: 'app-qr-scanner',
  standalone: true,
  imports: [CommonModule, ZXingScannerModule],
  templateUrl: './qr-scanner.html',
  styleUrl: './qr-scanner.scss'
})
export class QrScanner {  // ← No "Component" in class name
  formats = [BarcodeFormat.QR_CODE];

  isScanning = true;
  torchEnabled = false;
  hasTorch = false;
  usingFileUpload = false;
  uploadedImageUrl: string | null = null;
  scanResult: string | null = null;

  constructor(private router: Router) {}

  onScanSuccess(result: string) {
    this.scanResult = result;
    this.isScanning = false;
    this.usingFileUpload = false;

    const match = result.match(/verify\/([a-f0-9-]{36})/i);
    if (match) {
      this.router.navigate(['/verify', match[1]]);
    } else {
      alert('Invalid FarmChainX QR code');
      this.restart();
    }
  }

  onScanError(err: any) { console.error(err); }
  onPermission(granted: boolean) {
    if (!granted) {
      alert('Camera access denied. Using photo upload instead.');
      this.usingFileUpload = true;
    }
  }
  onCamerasFound(devices: MediaDeviceInfo[]) {
    this.hasTorch = devices.some(d => !!(d as any).getCapabilities?.()?.torch);
  }

  onFileSelected(event: Event) {
    const input = event.target as HTMLInputElement;
    if (!input.files?.length) return;

    const file = input.files[0];
    const reader = new FileReader();

    reader.onload = (e: any) => {
      this.uploadedImageUrl = e.target.result;
      this.isScanning = false;
      this.usingFileUpload = true;

      import('@zxing/library').then(zxing => {
        const codeReader = new zxing.BrowserQRCodeReader();
        const img = new Image();
        img.src = e.target.result;
        img.onload = () => {
          codeReader.decodeFromImageElement(img)
            .then(r => this.onScanSuccess(r.getText()))
            .catch(() => {
              alert('No QR code found');
              this.restart();
            });
        };
      });
    };
    reader.readAsDataURL(file);
  }

  restart() {
    this.isScanning = true;
    this.usingFileUpload = false;
    this.uploadedImageUrl = null;
    this.torchEnabled = false;
    this.scanResult = null;
  }
}

export default QrScanner;