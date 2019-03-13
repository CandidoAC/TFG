package ai.snips.snipsdemo;

import java.util.Date;

public class Event {
    private Date fecha;
    private String Nombre;

    public Event(Date fecha , String nombre) {
        this.fecha = fecha;
        Nombre = nombre;
    }

    public Date getFecha() {
        return fecha;
    }

    public void setFecha(Date fecha) {
        this.fecha = fecha;
    }

    public String getNombre() {
        return Nombre;
    }

    public void setNombre(String nombre) {
        Nombre = nombre;
    }
}
