package sv.edu.udb.ucacfcconnect.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Mantiene operativas las direcciones públicas anteriores mientras las páginas
 * estáticas se encuentran organizadas por módulo.
 */
@Controller
public class PageRedirectController {

    @GetMapping("/login.html")
    public String login() {
        return "redirect:/auth/login.html";
    }

    @GetMapping("/registro.html")
    public String registro() {
        return "redirect:/auth/registro.html";
    }

    @GetMapping("/auth-callback.html")
    public String callback() {
        return "redirect:/auth/callback.html";
    }

    @GetMapping("/perfil.html")
    public String perfil() {
        return "redirect:/cuenta/perfil.html";
    }

    @GetMapping("/cliente.html")
    public String cliente() {
        return "redirect:/cliente/index.html";
    }

    @GetMapping("/aprendizaje.html")
    public String aprendizaje() {
        return "redirect:/cliente/aprendizaje.html";
    }

    @GetMapping("/configuracion.html")
    public String configuracion() {
        return "redirect:/cliente/configuracion.html";
    }

    @GetMapping("/admin.html")
    public String administracion() {
        return "redirect:/admin/index.html";
    }

    @GetMapping("/cursos.html")
    public String cursos() {
        return "redirect:/admin/cursos.html";
    }
}
