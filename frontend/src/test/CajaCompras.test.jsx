/// <reference types="vitest" />
import React from "react";
import {
  render,
  screen,
  waitFor,
  fireEvent,
  act,
} from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { MemoryRouter } from "react-router-dom";
import { vi } from "vitest";
import CajaCompras from "../paginas/CajaCompras";

// 🔹 Mock de useNavigate
const mockNavigate = vi.fn();

vi.mock("react-router-dom", async () => {
  const actual = await vi.importActual("react-router-dom");
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

// 🔹 Mock de la API de clases
vi.mock("../api/adminCatalogos", () => ({
  clasesApi: {
    list: vi.fn().mockReturnValue({
      data: [
        { idClase: "1", nombre: "Turista" },
        { idClase: "2", nombre: "Ejecutiva" },
      ],
    }),
  },
}));

function renderCaja() {
  return render(
    <MemoryRouter>
      <CajaCompras />
    </MemoryRouter>
  );
}

describe("CajaCompras", () => {
  it("habilita el botón cuando el formulario está completo", async () => {
    const user = userEvent.setup();
    renderCaja();

    const boton = screen.getByRole("button", { name: /buscar vuelos/i });
    expect(boton).toBeDisabled();

    const inputDesde = screen.getByPlaceholderText(/ciudad o país/i);
    const inputHacia = screen.getByPlaceholderText(/destino/i);
    const fechaIda = screen.getByTestId("fecha-ida");
    const fechaVuelta = screen.getByTestId("fecha-vuelta");

    // llenar origen y destino
    await user.type(inputDesde, "Guatemala");
    await user.type(inputHacia, "Madrid");

    // llenar fechas correctamente (en los inputs de tipo date)
    act(() => {
      fireEvent.change(fechaIda, { target: { value: "2030-01-10" } });
      fireEvent.change(fechaVuelta, { target: { value: "2030-01-20" } });
    });

    // esperamos a que se cumpla canSubmit
    await waitFor(() => {
      expect(boton).toBeEnabled();
    });
  });

  it("navega a /vuelos con los parámetros correctos al enviar", async () => {
    const user = userEvent.setup();
    renderCaja();

    const boton = screen.getByRole("button", { name: /buscar vuelos/i });

    const inputDesde = screen.getByPlaceholderText(/ciudad o país/i);
    const inputHacia = screen.getByPlaceholderText(/destino/i);
    const fechaIda = screen.getByTestId("fecha-ida");
    const fechaVuelta = screen.getByTestId("fecha-vuelta");

    await user.type(inputDesde, "GUA");
    await user.type(inputHacia, "MIA");
    act(() => {
      fireEvent.change(fechaIda, { target: { value: "2030-02-01" } });
      fireEvent.change(fechaVuelta, { target: { value: "2030-02-10" } });
    });

    await waitFor(() => {
      expect(boton).toBeEnabled();
    });

    await user.click(boton);

    await waitFor(() => {
      expect(mockNavigate).toHaveBeenCalled();
    });

    const url = mockNavigate.mock.calls[0][0];

    expect(url).toContain("/vuelos?");
    expect(url).toContain("trip=round");
    expect(url).toContain("origen=GUA");
    expect(url).toContain("destino=MIA");
    expect(url).toContain("fsd=2030-02-01");
    expect(url).toContain("fsh=2030-02-01");
    expect(url).toContain("frd=2030-02-10");
    expect(url).toContain("frh=2030-02-10");
    expect(url).toContain("clase=");
    expect(url).toContain("pax=1");
  });
});
