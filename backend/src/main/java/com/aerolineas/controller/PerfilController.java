package com.aerolineas.controller;

import com.aerolineas.dao.PaisDAO;
import com.aerolineas.dao.PasajeroDAO;
import com.aerolineas.dao.UsuarioDAO;
import com.aerolineas.model.Pasajero;
import com.aerolineas.model.Usuario;
import com.aerolineas.dto.UsuarioAdminDTOs;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.time.Period;
import java.util.Map;

public class PerfilController {
  private final UsuarioDAO usuarios = new UsuarioDAO();
  private final PasajeroDAO pasajeros = new PasajeroDAO();
  private final PaisDAO paises = new PaisDAO();

  public record UpdPerfilReq(
      String nombres,
      String apellidos,
      String newPassword,     
      String fechaNacimiento, 
      Long idPais,            
      String pasaporte        
  ) {}

  public record PasajeroView(Long idPasajero, String fechaNacimiento, Integer edad,
                             Long idPais, String pasaporte) {}

  public record PerfilView(long idUsuario, String email, String nombres,
                           String apellidos, Integer idRol, PasajeroView pasajero){}

  private Integer edad(LocalDate f) {
    if (f == null) return null;
    return Period.between(f, LocalDate.now()).getYears();
  }

  @SuppressWarnings("unchecked")
  private Long userId(Context ctx) {
    Map<String, Object> claims = ctx.attribute("claims");
    if (claims == null) throw new IllegalStateException("no autenticado");
    return Long.parseLong(String.valueOf(claims.get("sub")));
  }

  public void getPerfil(Context ctx) {
    try {
      Long id = userId(ctx);
      Usuario u = usuarios.findById(id);
      Pasajero p = pasajeros.findByUsuario(id);

      PasajeroView pv = (p == null) ? null :
          new PasajeroView(
              p.getIdPasajero(),
              p.getFechaNacimiento() == null ? null : p.getFechaNacimiento().toString(),
              edad(p.getFechaNacimiento()),
              p.getIdPaisDocumento(),
              p.getPasaporte()
          );

      PerfilView out = new PerfilView(u.getIdUsuario(), u.getEmail(), u.getNombres(), u.getApellidos(), u.getIdRol(), pv);
      ctx.json(out);
    } catch (Exception e) {
      ctx.status(500).json(Map.of("error", "No se pudo obtener el perfil"));
    }
  }

  public void updatePerfil(Context ctx) {
    try {
      UpdPerfilReq b = ctx.bodyValidator(UpdPerfilReq.class)
          .check(x -> x.nombres()!=null && !x.nombres().isBlank(), "nombres requeridos")
          .check(x -> x.apellidos()!=null && !x.apellidos().isBlank(), "apellidos requeridos")
          .check(x -> x.pasaporte()==null || x.pasaporte().length() <= 20, "pasaporte demasiado largo")
          .get();

      Long id = userId(ctx);

      if (b.fechaNacimiento()!=null && !b.fechaNacimiento().isBlank()) {
        if (LocalDate.parse(b.fechaNacimiento()).isAfter(LocalDate.now())) {
          ctx.status(400).json(Map.of("error", "fecha de nacimiento inválida"));
          return;
        }
      }
      if (b.idPais()!=null && b.idPais() <= 0) {
        ctx.status(400).json(Map.of("error", "país inválido"));
        return;
      }

      var dto = new UsuarioAdminDTOs.UpdateSelf(
          b.nombres().trim(),
          b.apellidos().trim(),
          (b.newPassword()==null || b.newPassword().isBlank()) ? null : b.newPassword(),
          (b.fechaNacimiento()==null || b.fechaNacimiento().isBlank()) ? null : b.fechaNacimiento(),
          b.idPais(),
          b.pasaporte()==null ? null : b.pasaporte().trim()
      );

      usuarios.selfUpdate(id, dto);

      ctx.json(Map.of("ok", true));
    } catch (Exception e) {
      ctx.status(500).json(Map.of("error", "No se pudo guardar el perfil"));
    }
  }

  public void listPaises(Context ctx) {
    try {
      var list = paises.listAll();
      ctx.json(list);
    } catch (Exception e) {
      ctx.status(500).json(Map.of("error", "No se pudo listar países"));
    }
  }
}
