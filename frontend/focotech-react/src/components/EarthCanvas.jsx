import React, { useEffect, useRef } from 'react';

export default function EarthCanvas() {
  const canvasRef = useRef(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;
    const ctx = canvas.getContext('2d');
    let animationFrameId;

    // Handle high DPI
    const resize = () => {
      const rect = canvas.getBoundingClientRect();
      const dpr = window.devicePixelRatio || 1;
      canvas.width = rect.width * dpr;
      canvas.height = rect.height * dpr;
      ctx.scale(dpr, dpr);
    };

    resize();
    window.addEventListener('resize', resize);

    // Generate 3D sphere points (Fibonacci sphere distribution)
    const numPoints = 1400;
    const points = [];
    const goldenRatio = (1 + Math.sqrt(5)) / 2;

    for (let i = 0; i < numPoints; i++) {
      const theta = (2 * Math.PI * i) / goldenRatio;
      const phi = Math.acos(1 - (2 * (i + 0.5)) / numPoints);
      
      const x = Math.sin(phi) * Math.cos(theta);
      const y = Math.cos(phi);
      const z = Math.sin(phi) * Math.sin(theta);

      // Create continent-like clusters by modulating radius/brightness
      // Simulated landmass density pattern
      const noise = Math.sin(x * 3.5) * Math.cos(y * 3.5) * Math.sin(z * 3.5);
      const isLand = noise > -0.15;

      points.push({
        x,
        y,
        z,
        isLand,
        baseSize: isLand ? 1.4 : 0.85,
      });
    }

    // Latitude rings for that iconic sci-fi digital globe look
    const rings = [-0.7, -0.4, 0, 0.4, 0.7];
    const ringPoints = [];
    rings.forEach((lat) => {
      const r = Math.sqrt(Math.max(0, 1 - lat * lat));
      const steps = 60;
      for (let j = 0; j < steps; j++) {
        const rad = (j / steps) * Math.PI * 2;
        ringPoints.push({
          x: Math.cos(rad) * r,
          y: lat,
          z: Math.sin(rad) * r,
          isRing: true,
        });
      }
    });

    let rotationY = 0;
    const tiltAngle = 0.38; // ~22 degrees Earth tilt
    const cosTilt = Math.cos(tiltAngle);
    const sinTilt = Math.sin(tiltAngle);

    const render = () => {
      const width = canvas.getBoundingClientRect().width;
      const height = canvas.getBoundingClientRect().height;
      ctx.clearRect(0, 0, width, height);

      // Dynamic Globe center and radius
      const centerX = width * 0.48;
      const centerY = height * 0.52;
      const globeRadius = Math.min(width, height) * 0.42;

      // 1. Draw Subtle Atmospheric Back Glow
      const glowGrad = ctx.createRadialGradient(
        centerX,
        centerY,
        globeRadius * 0.8,
        centerX,
        centerY,
        globeRadius * 1.3
      );
      glowGrad.addColorStop(0, 'rgba(255, 90, 54, 0.08)');
      glowGrad.addColorStop(0.5, 'rgba(255, 122, 69, 0.04)');
      glowGrad.addColorStop(1, 'rgba(0, 0, 0, 0)');
      ctx.fillStyle = glowGrad;
      ctx.beginPath();
      ctx.arc(centerX, centerY, globeRadius * 1.35, 0, Math.PI * 2);
      ctx.fill();

      // 2. Outer Silhouette Rim Line
      ctx.strokeStyle = 'rgba(255, 255, 255, 0.06)';
      ctx.lineWidth = 1;
      ctx.beginPath();
      ctx.arc(centerX, centerY, globeRadius, 0, Math.PI * 2);
      ctx.stroke();

      // Rotation increment (smooth continuous spin)
      rotationY += 0.0025;
      const cosY = Math.cos(rotationY);
      const sinY = Math.sin(rotationY);

      // Render latitude rings
      ringPoints.forEach((pt) => {
        // Rotate around Y
        const rx = pt.x * cosY + pt.z * sinY;
        const rz = -pt.x * sinY + pt.z * cosY;
        // Tilt around Z
        const x = rx * cosTilt - pt.y * sinTilt;
        const y = rx * sinTilt + pt.y * cosTilt;
        const z = rz;

        if (z > -0.2) {
          const alpha = (z + 0.2) * 0.15;
          ctx.fillStyle = `rgba(255, 255, 255, ${alpha.toFixed(3)})`;
          const px = centerX + x * globeRadius;
          const py = centerY + y * globeRadius;
          ctx.beginPath();
          ctx.arc(px, py, 0.6, 0, Math.PI * 2);
          ctx.fill();
        }
      });

      // 3. Render 3D Earth Particles
      points.forEach((pt) => {
        // Rotate Y (Spin)
        const rx = pt.x * cosY + pt.z * sinY;
        const rz = -pt.x * sinY + pt.z * cosY;

        // Tilt (Axial inclination)
        const x = rx * cosTilt - pt.y * sinTilt;
        const y = rx * sinTilt + pt.y * cosTilt;
        const z = rz;

        // Perspective depth calculation
        const px = centerX + x * globeRadius;
        const py = centerY + y * globeRadius;

        if (z > 0) {
          // Front-facing points: high brightness, depth-scaled
          const depth = (z + 1) / 2; // 0.5 to 1
          const alpha = pt.isLand ? 0.35 + depth * 0.55 : 0.15 + depth * 0.3;
          const dotSize = pt.baseSize * (0.8 + depth * 0.5);

          // Subtle theme tint (warm luminous dots)
          if (pt.isLand) {
            ctx.fillStyle = `rgba(255, 255, 255, ${alpha.toFixed(3)})`;
          } else {
            ctx.fillStyle = `rgba(255, 122, 69, ${(alpha * 0.7).toFixed(3)})`;
          }

          ctx.beginPath();
          ctx.arc(px, py, dotSize, 0, Math.PI * 2);
          ctx.fill();
        } else {
          // Back hemisphere points: soft dim silhouettes
          const depth = (z + 1) / 2; // 0 to 0.5
          const alpha = depth * 0.12;
          ctx.fillStyle = `rgba(255, 255, 255, ${alpha.toFixed(3)})`;
          ctx.beginPath();
          ctx.arc(px, py, 0.6, 0, Math.PI * 2);
          ctx.fill();
        }
      });

      animationFrameId = requestAnimationFrame(render);
    };

    render();

    return () => {
      window.removeEventListener('resize', resize);
      cancelAnimationFrame(animationFrameId);
    };
  }, []);

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 w-full h-full pointer-events-none select-none z-0"
    />
  );
}
