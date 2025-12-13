/// <reference types="vitest" />
import React from "react";
import { render, screen, waitFor } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import Carrito from "../paginas/Carrito";
import { comprasApi } from "../api/compras";
import { getUser } from "../lib/auth";

//  Mock de useNavigate
const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

//  Mock de la API de compras
vi.mock("../api/compras", () => ({
  comprasApi: {
    getCart: vi.fn(),
    updateItem: vi.fn(),
    removeItem: vi.fn(),
  },
}));

//  Mock de auth
vi.mock("../lib/auth", () => ({
  getUser: vi.fn(),
}));

// (Opcional) Mock sencillo de CartItem por si es muy complejo
vi.mock("../paginas/CartItem", () => ({
  __esModule: true,
  default: ({ item }) => <li>{item.nombre}</li>,
}));

describe("Carrito", () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it("redirige a /login si no hay usuario autenticado", async () => {
    // getUser devuelve null ➜ no logueado
    getUser.mockReturnValue(null);

    // Igual mockeamos getCart para que no truene si se llama
    comprasApi.getCart.mockResolvedValue({
      data: { items: [], total: 0 },
    });

    render(
      <MemoryRouter>
        <Carrito />
      </MemoryRouter>
    );

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalledWith("/login");
    });
  });

  it("muestra el total y navega a checkout cuando hay items y se hace clic en Ir a pagar", async () => {
    const user = userEvent.setup();

    // Usuario logueado
    getUser.mockReturnValue({
      idUsuario: 1,
      email: "test@unis.edu.gt",
    });

    // Carrito con 1 item
    comprasApi.getCart.mockResolvedValue({
      data: {
        items: [
          {
            idItem: 1,
            nombre: "Vuelo de prueba",
            precio: 1000,
            cantidad: 1,
          },
        ],
        total: 1000,
      },
    });

    render(
      <MemoryRouter>
        <Carrito />
      </MemoryRouter>
    );

    // Esperamos a que deje de cargar y se muestre el total
    await waitFor(() => {
      expect(screen.getByText(/Total:/i)).toBeInTheDocument();
    });

    const botonPagar = screen.getByRole("button", { name: /ir a pagar/i });

    await user.click(botonPagar);

    expect(mockNavigate).toHaveBeenCalledWith("/compras/checkout");
  });
});
