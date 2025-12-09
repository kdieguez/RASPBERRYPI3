/// <reference types="vitest" />
import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi, describe, it, expect, beforeEach } from "vitest";

import Checkout from "../paginas/Checkout";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";

// 🔹 Mock de useNavigate
const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// 🔹 Mock de API de compras
vi.mock("../api/compras", () => ({
  comprasApi: {
    getCart: vi.fn(),
    checkout: vi.fn(),
  },
}));

// 🔹 Mock de auth
vi.mock("../lib/auth", () => ({
  getUser: vi.fn(),
}));

// 🔹 Mock de RouteLine (no nos importa la lógica interna aquí)
vi.mock("../components/RouteLine", () => ({
  __esModule: true,
  default: () => <div data-testid="route-line" />,
}));

// 🔹 Mock de alert
global.alert = vi.fn();

describe("Checkout", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("redirige a /login si no hay usuario autenticado", async () => {
    getUser.mockReturnValue(null);

    // Por si el componente intenta consultar el carrito
    comprasApi.getCart.mockResolvedValue({
      data: { items: [], total: 0 },
    });

    render(
      <MemoryRouter>
        <Checkout />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/login");
    });
  });

  it(
    "realiza checkout exitoso y navega a /compras/checkout/:idReserva",
    async () => {
      const user = userEvent.setup();

      // Usuario logueado
      getUser.mockReturnValue({
        idUsuario: 1,
        nombre: "Juan Pérez",
        email: "juan@example.com",
      });

      // Carrito con items
      comprasApi.getCart.mockResolvedValue({
        data: {
          items: [
            {
              idItem: 1,
              codigoVuelo: "UN123",
              clase: "Turista",
              fechaSalida: "2030-01-10T10:00:00Z",
              subtotal: 1000,
            },
          ],
          total: 1000,
        },
      });

      // Respuesta de checkout
      comprasApi.checkout.mockResolvedValue({
        data: { idReserva: 999 },
      });

      render(
        <MemoryRouter>
          <Checkout />
        </MemoryRouter>
      );

      // Esperar a que se cargue el resumen (loading = false)
      await waitFor(() => {
        expect(screen.getByText(/resumen/i)).toBeInTheDocument();
      });

      const inputNumero = screen.getByLabelText(/número de tarjeta/i);
      const inputMes = screen.getByLabelText(/mes \(mm\)/i);
      const inputAnio = screen.getByLabelText(/año \(yy\/aaaa\)/i);
      const inputCvv = screen.getByLabelText(/cvv/i);
      const inputDireccion = screen.getByLabelText(/dirección/i);
      const inputCiudad = screen.getByLabelText(/ciudad/i);
      const inputPais = screen.getByLabelText(/país/i);
      const inputZip = screen.getByLabelText(/código postal/i);

      const botonPagar = screen.getByRole("button", {
        name: /confirmar y pagar/i,
      });

      // Llenar datos válidos
      await user.type(inputNumero, "4111 1111 1111 1111"); // Luhn OK
      await user.type(inputMes, "12");
      await user.type(inputAnio, "50"); // 2050
      await user.type(inputCvv, "123");
      await user.type(inputDireccion, "Calle 1 zona 1");
      await user.type(inputCiudad, "Guatemala");
      await user.type(inputPais, "Guatemala");
      await user.type(inputZip, "01001");

      // (Opcional, pero ayuda a que quede claro)
      await waitFor(() => {
        expect(botonPagar).not.toBeDisabled();
      });

      await user.click(botonPagar);

      // Esperar a que se llame checkout
      await waitFor(() => {
        expect(comprasApi.checkout).toHaveBeenCalledTimes(1);
      });

      const payload = comprasApi.checkout.mock.calls[0][0];

      expect(payload).toMatchObject({
        tarjeta: {
          nombre: "Juan Pérez",
          numero: "4111111111111111",
          expMes: 12,
          expAnio: 2050,
          cvv: "123",
        },
        facturacion: {
          direccion: "Calle 1 zona 1",
          ciudad: "Guatemala",
          pais: "Guatemala",
          zip: "01001",
        },
      });

      expect(mockNavigate).toHaveBeenCalledWith("/compras/checkout/999");
    },
    15000 // ⬅⬅⬅ AQUÍ está el timeout de 15s para ESTE test
  );
});
