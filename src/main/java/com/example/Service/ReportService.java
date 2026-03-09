package com.example.Service;

import com.example.DTOs.ArenaResponseDTO;
import com.example.DTOs.FaturamentoDTO;
import com.example.DTOs.FinanceiroDashboardDTO;
import com.example.DTOs.UserResponseDTO;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.OutputStream;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
public class ReportService {

    @Autowired private ArenaService       arenaService;
    @Autowired private UserService        userService;
    @Autowired private AgendamentoService agendamentoService;
    @Autowired private AsaasService       asaasService;

    private static final Color BLACK      = Color.BLACK;
    private static final Color WHITE      = Color.WHITE;
    private static final Color LIGHT_GRAY = new Color(240, 240, 240);
    private static final Color MID_GRAY   = new Color(180, 180, 180);
    private static final Color DARK_GRAY  = new Color(80,  80,  80);

    private Font fTitle()    { return new Font(Font.HELVETICA, 18, Font.BOLD,   BLACK);     }
    private Font fSection()  { return new Font(Font.HELVETICA, 11, Font.BOLD,   BLACK);     }
    private Font fSubtitle() { return new Font(Font.HELVETICA, 10, Font.NORMAL, DARK_GRAY); }
    private Font fHeader()   { return new Font(Font.HELVETICA,  8, Font.BOLD,   WHITE);     }
    private Font fCell()     { return new Font(Font.HELVETICA,  8, Font.NORMAL, BLACK);     }
    private Font fCellBold() { return new Font(Font.HELVETICA,  8, Font.BOLD,   BLACK);     }
    private Font fSmall()    { return new Font(Font.HELVETICA,  7, Font.NORMAL, DARK_GRAY); }
    private Font fKpiVal()   { return new Font(Font.HELVETICA, 13, Font.BOLD,   BLACK);     }
    private Font fKpiLbl()   { return new Font(Font.HELVETICA,  7, Font.NORMAL, DARK_GRAY); }

    public void gerarRelatorioSuperAdmin(OutputStream out) {
        Document doc = null;
        try {
            List<ArenaResponseDTO> arenas  = arenaService.findAllAdmin();
            List<UserResponseDTO>  users   = userService.findAll();
            FinanceiroDashboardDTO finance = asaasService.getFinanceiroDashboard();

            doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            addPageFooter(writer);
            doc.open();

            // Header
            addHeader(doc, "Relatório Geral – Super Admin", "Gerado em " + today());

            long ativas   = arenas.stream().filter(a -> Boolean.TRUE.equals(a.ativo())).count();
            long inativas = arenas.size() - ativas;
            long admins   = users.stream().filter(u -> "ADMIN".equals(str(u.getRole()))).count();
            long clientes = users.stream().filter(u -> "CLIENTE".equals(str(u.getRole()))).count();

            addKpiRow(doc, new String[][]{
                    {"Total de Arenas",  str(arenas.size())},
                    {"Arenas Ativas",    str(ativas)},
                    {"Arenas Inativas",  str(inativas)},
                    {"Total Usuários",   str(users.size())},
                    {"Admins",           str(admins)},
                    {"Clientes",         str(clientes)}
            });

            addSectionTitle(doc, "Resumo Financeiro");
            addKpiRow(doc, new String[][]{
                    {"Saldo Total (Asaas)",  cur(finance.getFaturamentoTotal())},
                    {"A Receber",            cur(finance.getAReceber())},
                    {"Lucro – Splits",       cur(finance.getLucroSplit())},
                    {"Lucro – Assinaturas",  cur(finance.getLucroAssinatura())}
            });

            if (finance.getTransacoes() != null && !finance.getTransacoes().isEmpty()) {
                addSectionTitle(doc, "Últimas Transações");
                PdfPTable t = table(new float[]{1.2f, 2.5f, 3f, 1.5f, 1.3f});
                addHeaders(t, "Data", "Cliente", "Descrição", "Valor", "Status");
                finance.getTransacoes().forEach(tx -> addRow(t,
                        fmtDate(tx.getData()),
                        nvl(tx.getCliente()),
                        nvl(tx.getDescricao()),
                        cur(tx.getValor()),
                        nvl(tx.getStatus())
                ));
                doc.add(t);
                spacer(doc);
            }

            addSectionTitle(doc, "Arenas Cadastradas");
            PdfPTable arenaTable = table(new float[]{2f, 1.8f, 1.3f, 1.8f, 1f, 2f, 2f});
            addHeaders(arenaTable, "Nome", "CNPJ", "CEP", "Cidade / UF", "Status", "Responsável", "E-mail");
            for (ArenaResponseDTO a : arenas) {
                addRow(arenaTable,
                        nvl(a.nome()),
                        fmtCnpj(a.cnpj()),
                        fmtCep(a.cep()),
                        nvl(a.cidade()) + (a.estado() != null ? " / " + a.estado() : ""),
                        Boolean.TRUE.equals(a.ativo()) ? "Ativa" : "Inativa",
                        nvl(a.adminNome()),
                        nvl(a.adminEmail())
                );
            }
            doc.add(arenaTable);
            spacer(doc);

            addSectionTitle(doc, "Usuários Cadastrados");
            PdfPTable userTable = table(new float[]{2f, 2.5f, 1.6f, 1.6f, 1f, 0.9f});
            addHeaders(userTable, "Nome", "E-mail", "CPF", "Telefone", "Perfil", "Status");
            for (UserResponseDTO u : users) {
                addRow(userTable,
                        nvl(u.getNome()),
                        nvl(u.getEmail()),
                        fmtCpf(u.getCpf()),
                        fmtTel(u.getTelefone()),
                        str(u.getRole()),
                        Boolean.TRUE.equals(u.getAtivo()) ? "Ativo" : "Inativo"
                );
            }
            doc.add(userTable);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório SuperAdmin: " + e.getMessage(), e);
        } finally {
            if (doc != null && doc.isOpen()) {
                doc.close();
            }
        }
    }

    public void gerarRelatorioDashboard(int ano, OutputStream out) {
        Document doc = null;
        try {
            List<FaturamentoDTO> faturamento = agendamentoService.findFaturamentoAnual(ano);

            double total  = faturamento.stream().mapToDouble(FaturamentoDTO::getValor).sum();
            double melhor = faturamento.stream().mapToDouble(FaturamentoDTO::getValor).max().orElse(0);
            long   ativos = faturamento.stream().filter(f -> f.getValor() > 0).count();
            double media  = ativos > 0 ? total / ativos : 0;

            doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            addPageFooter(writer);
            doc.open();

            addHeader(doc, "Relatório de Dashboard – " + ano, "Gerado em " + today());

            addKpiRow(doc, new String[][]{
                    {"Faturamento Total", cur(total)},
                    {"Melhor Mês",        cur(melhor)},
                    {"Média Mensal",      cur(media)},
                    {"Meses com Receita", ativos + " / 12"}
            });

            addSectionTitle(doc, "Faturamento Mensal");
            PdfPTable bt = table(new float[]{1.5f, 2.5f, 1.5f});
            addHeaders(bt, "Mês", "Faturamento", "% do Total");

            String[] months = {"Janeiro","Fevereiro","Março","Abril","Maio","Junho",
                    "Julho","Agosto","Setembro","Outubro","Novembro","Dezembro"};

            for (int i = 0; i < faturamento.size(); i++) {
                double val = faturamento.get(i).getValor();
                double pct = total > 0 ? (val / total) * 100 : 0;

                PdfPCell mc = cell(months[i], fCell());
                PdfPCell vc = cell(cur(val),  val > 0 ? fCellBold() : fSmall());
                PdfPCell pc = cell(val > 0 ? String.format("%.1f%%", pct) : "—", fSmall());
                vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
                pc.setHorizontalAlignment(Element.ALIGN_CENTER);

                if (val == melhor && melhor > 0) {
                    mc.setBackgroundColor(LIGHT_GRAY);
                    vc.setBackgroundColor(LIGHT_GRAY);
                    pc.setBackgroundColor(LIGHT_GRAY);
                }

                bt.addCell(mc); bt.addCell(vc); bt.addCell(pc);
            }

            PdfPCell tl = cell("TOTAL",  fCellBold()); tl.setBackgroundColor(LIGHT_GRAY);
            PdfPCell tv = cell(cur(total), fCellBold()); tv.setBackgroundColor(LIGHT_GRAY); tv.setHorizontalAlignment(Element.ALIGN_RIGHT);
            PdfPCell tp = cell("100%",   fCellBold()); tp.setBackgroundColor(LIGHT_GRAY); tp.setHorizontalAlignment(Element.ALIGN_CENTER);
            bt.addCell(tl); bt.addCell(tv); bt.addCell(tp);

            doc.add(bt);
            spacer(doc);

            addSectionTitle(doc, "Gráfico de Faturamento");
            addTextBarChart(doc, faturamento, melhor);

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório Dashboard: " + e.getMessage(), e);
        } finally {
            if (doc != null && doc.isOpen()) {
                doc.close();
            }
        }
    }

    private void addHeader(Document doc, String title, String subtitle) throws DocumentException {
        Paragraph t = new Paragraph("Arena Connect", fTitle());
        t.setSpacingAfter(2);
        doc.add(t);

        Paragraph s = new Paragraph(title, fSection());
        s.setSpacingAfter(2);
        doc.add(s);

        Paragraph sub = new Paragraph(subtitle, fSubtitle());
        sub.setSpacingAfter(8);
        doc.add(sub);

        PdfPTable line = new PdfPTable(1);
        line.setWidthPercentage(100);
        PdfPCell lc = new PdfPCell();
        lc.setFixedHeight(1.5f);
        lc.setBackgroundColor(BLACK);
        lc.setBorder(Rectangle.NO_BORDER);
        line.addCell(lc);
        line.setSpacingAfter(12);
        doc.add(line);
    }

    private void addKpiRow(Document doc, String[][] kpis) throws DocumentException {
        PdfPTable t = new PdfPTable(kpis.length);
        t.setWidthPercentage(100);
        t.setSpacingAfter(10);

        for (String[] kpi : kpis) {
            PdfPCell c = new PdfPCell();
            c.setBorder(Rectangle.BOX);
            c.setBorderColor(MID_GRAY);
            c.setBorderWidth(0.5f);
            c.setBackgroundColor(LIGHT_GRAY);
            c.setPadding(8);
            c.setHorizontalAlignment(Element.ALIGN_CENTER);

            Paragraph val = new Paragraph(kpi[1], fKpiVal());
            val.setAlignment(Element.ALIGN_CENTER);
            Paragraph lbl = new Paragraph(kpi[0], fKpiLbl());
            lbl.setAlignment(Element.ALIGN_CENTER);

            c.addElement(val);
            c.addElement(lbl);
            t.addCell(c);
        }
        doc.add(t);
    }

    private void addSectionTitle(Document doc, String text) throws DocumentException {
        Paragraph p = new Paragraph(text, fSection());
        p.setSpacingBefore(10);
        p.setSpacingAfter(5);
        doc.add(p);
    }

    private PdfPTable table(float[] widths) throws DocumentException {
        PdfPTable t = new PdfPTable(widths.length);
        t.setWidthPercentage(100);
        t.setWidths(widths);
        t.setSpacingAfter(6);
        t.setHeaderRows(1);
        return t;
    }

    private void addHeaders(PdfPTable t, String... headers) {
        for (String h : headers) {
            PdfPCell c = new PdfPCell(new Phrase(h, fHeader()));
            c.setBackgroundColor(BLACK);
            c.setPadding(5);
            c.setBorder(Rectangle.NO_BORDER);
            t.addCell(c);
        }
    }

    private void addRow(PdfPTable t, String... values) {
        boolean even = t.size() % 2 == 0;
        Color   bg   = even ? WHITE : LIGHT_GRAY;
        for (String v : values) {
            PdfPCell c = cell(nvl(v), fCell());
            c.setBackgroundColor(bg);
            t.addCell(c);
        }
    }

    private PdfPCell cell(String text, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(text, font));
        c.setPadding(4);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(MID_GRAY);
        c.setBorderWidthBottom(0.4f);
        return c;
    }

    private void addTextBarChart(Document doc, List<FaturamentoDTO> data, double max)
            throws DocumentException {
        if (max <= 0) {
            doc.add(new Paragraph("Sem dados para exibir.", fSmall()));
            return;
        }
        String[] m = {"Jan","Fev","Mar","Abr","Mai","Jun","Jul","Ago","Set","Out","Nov","Dez"};
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            double v      = data.get(i).getValor();
            int    blocks = (int) Math.round((v / max) * 20);
            String bar    = "|".repeat(Math.max(blocks, v > 0 ? 1 : 0));
            sb.append(String.format("%-3s  %-20s  %s%n", m[i], bar, v > 0 ? cur(v) : "—"));
        }

        PdfPTable wrap = new PdfPTable(1);
        wrap.setWidthPercentage(100);
        PdfPCell wc = new PdfPCell();
        wc.setBorder(Rectangle.BOX);
        wc.setBorderColor(MID_GRAY);
        wc.setBorderWidth(0.5f);
        wc.setBackgroundColor(LIGHT_GRAY);
        wc.setPadding(10);
        wc.addElement(new Paragraph(sb.toString(), new Font(Font.COURIER, 8, Font.NORMAL, BLACK)));
        wrap.addCell(wc);
        doc.add(wrap);
    }

    private void addPageFooter(PdfWriter writer) {
        writer.setPageEvent(new PdfPageEventHelper() {
            @Override
            public void onEndPage(PdfWriter w, Document d) {
                PdfContentByte cb = w.getDirectContent();
                Phrase footer = new Phrase(
                        "Arena Connect  •  Página " + w.getPageNumber() + "  •  " + today(),
                        new Font(Font.HELVETICA, 7, Font.NORMAL, DARK_GRAY));
                ColumnText.showTextAligned(cb, Element.ALIGN_CENTER, footer,
                        (d.right() - d.left()) / 2 + d.leftMargin(),
                        d.bottom() - 12, 0);
                cb.setColorStroke(MID_GRAY);
                cb.setLineWidth(0.5f);
                cb.moveTo(d.left(),  d.bottom() - 5);
                cb.lineTo(d.right(), d.bottom() - 5);
                cb.stroke();
            }
        });
    }

    private void spacer(Document doc) throws DocumentException { doc.add(Chunk.NEWLINE); }

    private String nvl(String s)  { return s != null && !s.isBlank() ? s : "—"; }
    private String str(Object o)  { return o != null ? o.toString() : "—"; }
    private String today()        { return LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")); }

    private String cur(Double v) {
        if (v == null) return "—";
        return NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(v);
    }

    private String fmtDate(String d) {
        if (d == null || d.isBlank()) return "—";
        try {
            String[] p = d.split("T")[0].split("-");
            return p[2] + "/" + p[1] + "/" + p[0];
        } catch (Exception e) { return d; }
    }

    private String fmtCnpj(String c) {
        if (c == null) return "—";
        String d = c.replaceAll("\\D", "");
        return d.length() == 14
                ? d.replaceAll("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5") : c;
    }

    private String fmtCpf(String c) {
        if (c == null) return "—";
        String d = c.replaceAll("\\D", "");
        return d.length() == 11
                ? d.replaceAll("(\\d{3})(\\d{3})(\\d{3})(\\d{2})", "$1.$2.$3-$4") : c;
    }

    private String fmtCep(String c) {
        if (c == null) return "—";
        String d = c.replaceAll("\\D", "");
        return d.length() == 8 ? d.replaceAll("(\\d{5})(\\d{3})", "$1-$2") : c;
    }

    private String fmtTel(String t) {
        if (t == null) return "—";
        String d = t.replaceAll("\\D", "");
        if (d.length() == 11) return d.replaceAll("(\\d{2})(\\d{5})(\\d{4})", "($1) $2-$3");
        if (d.length() == 10) return d.replaceAll("(\\d{2})(\\d{4})(\\d{4})", "($1) $2-$3");
        return t;
    }

    public void gerarRelatorioAgendamentos(Integer idQuadra, LocalDate data,String statusFiltro, OutputStream out) {
        Document doc = null;
        try {
            List<com.example.Models.Agendamentos> todos =
                    agendamentoService.findAllAgendamentos(idQuadra, data);

            List<com.example.Models.Agendamentos> agendamentos = (statusFiltro != null && !statusFiltro.isBlank())
                    ? todos.stream().filter(a -> statusFiltro.equalsIgnoreCase(a.getStatus())).toList()
                    : todos;

            long total       = agendamentos.size();
            long confirmados = agendamentos.stream().filter(a -> "CONFIRMADO".equals(a.getStatus())).count();
            long finalizados = agendamentos.stream().filter(a -> "FINALIZADO".equals(a.getStatus())).count();
            long cancelados  = agendamentos.stream().filter(a -> "CANCELADO" .equals(a.getStatus())).count();
            long pendentes   = agendamentos.stream().filter(a -> "PENDENTE"  .equals(a.getStatus())).count();

            double receitaTotal = agendamentos.stream()
                    .filter(a -> "CONFIRMADO".equals(a.getStatus()) || "FINALIZADO".equals(a.getStatus()))
                    .mapToDouble(a -> a.getValor() != null ? a.getValor() : 0)
                    .sum();
            double ticketMedio = (confirmados + finalizados) > 0 ? receitaTotal / (confirmados + finalizados) : 0;

            StringBuilder sub = new StringBuilder("Gerado em " + today());
            if (data != null)
                sub.append("  •  Data: ").append(data.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            if (statusFiltro != null && !statusFiltro.isBlank())
                sub.append("  •  Status: ").append(statusFiltro);

            doc = new Document(PageSize.A4, 36, 36, 50, 40);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            addPageFooter(writer);
            doc.open();

            addHeader(doc, "Relatório de Agendamentos", sub.toString());

            addSectionTitle(doc, "Resumo do Período");
            addKpiRow(doc, new String[][]{
                    {"Total",       str(total)},
                    {"Confirmadas", str(confirmados)},
                    {"Finalizadas", str(finalizados)},
                    {"Pendentes",   str(pendentes)},
                    {"Canceladas",  str(cancelados)}
            });
            addKpiRow(doc, new String[][]{
                    {"Receita (confirmadas + finalizadas)", cur(receitaTotal)},
                    {"Ticket Médio",                        cur(ticketMedio)}
            });

            addSectionTitle(doc, "Listagem de Agendamentos");

            if (agendamentos.isEmpty()) {
                doc.add(new Paragraph("Nenhum agendamento encontrado para os filtros selecionados.", fSmall()));
            } else {
                PdfPTable t = table(new float[]{1.3f, 1.2f, 2.4f, 2f, 1.2f, 1.2f});
                addHeaders(t, "Data", "Horário", "Cliente", "Quadra", "Valor", "Status");

                int row = 0;
                for (com.example.Models.Agendamentos a : agendamentos) {
                    String dataFmt = a.getData_inicio() != null
                            ? a.getData_inicio().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "—";
                    String horario = agendamentoHorario(a.getData_inicio(), a.getData_fim());
                    String cliente = nvl(a.getNomeCliente())
                            + (a.getNumeroCliente() != null ? "\n" + a.getNumeroCliente() : "");
                    String quadra  = nvl(a.getQuadraNome());
                    double valor   = a.getValor() != null ? a.getValor() : 0;
                    String status  = nvl(a.getStatus());
                    boolean pago   = "CONFIRMADO".equals(status) || "FINALIZADO".equals(status);

                    Color bg = (row % 2 == 0) ? WHITE : LIGHT_GRAY;

                    PdfPCell cData = cell(dataFmt,                       fCell());
                    PdfPCell cHor  = cell(horario,                       fCell());
                    PdfPCell cCli  = cell(cliente,                       fSmall());
                    PdfPCell cQua  = cell(quadra,                        fCell());
                    PdfPCell cVal  = cell(valor > 0 ? cur(valor) : "—", pago ? fCellBold() : fCell());
                    PdfPCell cSts  = agendamentoStatusCell(status);

                    cHor.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cVal.setHorizontalAlignment(Element.ALIGN_RIGHT);
                    for (PdfPCell c : new PdfPCell[]{cData, cHor, cCli, cQua, cVal}) c.setBackgroundColor(bg);
                    cSts.setBackgroundColor(bg);

                    t.addCell(cData); t.addCell(cHor); t.addCell(cCli);
                    t.addCell(cQua);  t.addCell(cVal); t.addCell(cSts);
                    row++;
                }

                Color totBg = LIGHT_GRAY;
                PdfPCell ct0 = cell("TOTAL",                       fCellBold()); ct0.setBackgroundColor(totBg);
                PdfPCell ct1 = cell("",                            fCell());     ct1.setBackgroundColor(totBg);
                PdfPCell ct2 = cell(total + " agendamento(s)",     fCellBold()); ct2.setBackgroundColor(totBg);
                PdfPCell ct3 = cell("",                            fCell());     ct3.setBackgroundColor(totBg);
                PdfPCell ct4 = cell(cur(receitaTotal),             fCellBold()); ct4.setBackgroundColor(totBg);
                PdfPCell ct5 = cell("",                            fCell());     ct5.setBackgroundColor(totBg);
                ct4.setHorizontalAlignment(Element.ALIGN_RIGHT);
                t.addCell(ct0); t.addCell(ct1); t.addCell(ct2);
                t.addCell(ct3); t.addCell(ct4); t.addCell(ct5);

                doc.add(t);
            }

            doc.close();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao gerar relatório de agendamentos: " + e.getMessage(), e);
        } finally {
            if (doc != null && doc.isOpen()) {
                doc.close();
            }
        }
    }

    private String agendamentoHorario(java.time.LocalDateTime inicio, java.time.LocalDateTime fim) {
        try {
            String hi = inicio != null ? inicio.format(DateTimeFormatter.ofPattern("HH:mm")) : "—";
            String hf = fim    != null ? fim   .format(DateTimeFormatter.ofPattern("HH:mm")) : "—";
            return hi + " – " + hf;
        } catch (Exception e) { return "—"; }
    }

    private PdfPCell agendamentoStatusCell(String status) {
        Font f = switch (status.toUpperCase()) {
            case "CONFIRMADO" -> new Font(Font.HELVETICA, 8, Font.BOLD,   new Color(30, 120, 60));
            case "FINALIZADO" -> new Font(Font.HELVETICA, 8, Font.BOLD,   DARK_GRAY);
            case "CANCELADO"  -> new Font(Font.HELVETICA, 8, Font.BOLD,   new Color(160, 40, 40));
            case "PENDENTE"   -> new Font(Font.HELVETICA, 8, Font.NORMAL, new Color(140, 100, 0));
            default           -> fCell();
        };
        PdfPCell c = new PdfPCell(new Phrase(status, f));
        c.setPadding(4);
        c.setBorder(Rectangle.BOTTOM);
        c.setBorderColor(MID_GRAY);
        c.setBorderWidthBottom(0.4f);
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        return c;
    }
}