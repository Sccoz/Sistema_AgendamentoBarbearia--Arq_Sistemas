/** Apresentação: renderiza o catálogo e as reservas recebidas da API Java. */
(() => {
  "use strict";

  const $ = (id) => document.getElementById(id);
  const SETTINGS_KEY = "navalha.agenda.preferences.v1";
  const money = (value) =>
    Number(value).toLocaleString("pt-BR", {
      style: "currency",
      currency: "BRL",
    });
  const compactMoney = (value) =>
    Number(value).toLocaleString("pt-BR", {
      style: "currency",
      currency: "BRL",
      maximumFractionDigits: 0,
    });
  const dayKey = (date) =>
    `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
  const today = () =>
    new Intl.DateTimeFormat("sv-SE", {
      timeZone: "America/Sao_Paulo",
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
    }).format(new Date());
  const displayDate = (date) =>
    new Date(`${date}T12:00:00`).toLocaleDateString("pt-BR");
  const initials = (name) =>
    name
      .trim()
      .split(/\s+/)
      .slice(0, 2)
      .map((word) => word[0])
      .join("")
      .toUpperCase();
  const element = (tag, className, text) => {
    const node = document.createElement(tag);
    node.className = className;
    node.textContent = text;
    return node;
  };

  let selected = today();
  let barber = "todos";
  let month;
  let catalog = { servicos: [], barbeiros: [] };
  let rows = [];
  let detailId = null;
  let toastTimer;
  let listRequest = 0;
  let slotsRequest = 0;
  let saving = false;
  let canceling = false;
  let ready = false;

  // Apenas preferências de interface. Agendamentos são mantidos exclusivamente pelo backend.
  try {
    const saved = JSON.parse(localStorage.getItem(SETTINGS_KEY) || "{}");
    if (
      typeof saved.date === "string" &&
      /^\d{4}-\d{2}-\d{2}$/.test(saved.date) &&
      !Number.isNaN(new Date(`${saved.date}T12:00:00`).getTime())
    )
      selected = saved.date;
    if (typeof saved.barber === "string") barber = saved.barber;
  } catch {
    /* Sem armazenamento local, a agenda continua funcionando pela API. */
  }
  month = new Date(`${selected}T12:00:00`);
  month.setDate(1);

  function savePreferences() {
    try {
      localStorage.setItem(
        SETTINGS_KEY,
        JSON.stringify({ date: selected, barber }),
      );
    } catch {
      /* Preferências são opcionais. */
    }
  }

  function notify(text) {
    $("toast").textContent = text;
    $("toast").hidden = false;
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => {
      $("toast").hidden = true;
    }, 6000);
  }

  function connection(ok) {
    const badge = $("connection-status");
    badge.classList.toggle("connection-error", !ok);
    badge.replaceChildren(
      element("span", "", ""),
      document.createTextNode(
        ok ? "Agenda sincronizada" : "Sem conexão com a agenda",
      ),
    );
  }

  function selectSection(name) {
    ["agenda", "servicos", "equipe"].forEach((key) => {
      $(`${key}-section`).hidden = key !== name;
    });
    document
      .querySelectorAll(".nav-item")
      .forEach((button) =>
        button.classList.toggle("active", button.dataset.section === name),
      );
    $("breadcrumb").textContent = {
      agenda: "Agenda",
      servicos: "Serviços",
      equipe: "Equipe",
    }[name];
  }

  function onDate(date) {
    selected = date;
    month = new Date(`${date}T12:00:00`);
    month.setDate(1);
    savePreferences();
    render();
  }

  function renderCalendar() {
    $("calendar-month").textContent = month.toLocaleDateString("pt-BR", {
      month: "long",
      year: "numeric",
    });
    $("calendar-grid").replaceChildren();
    const first = new Date(month);
    first.setDate(1 - first.getDay());
    const occupiedDates = new Set(rows.map((row) => row.date));
    for (let i = 0; i < 42; i++) {
      const date = new Date(first);
      date.setDate(first.getDate() + i);
      const key = dayKey(date);
      const button = element("button", "", String(date.getDate()));
      button.type = "button";
      button.classList.toggle(
        "other-month",
        date.getMonth() !== month.getMonth(),
      );
      button.classList.toggle("selected", key === selected);
      button.classList.toggle("has-bookings", occupiedDates.has(key));
      button.setAttribute(
        "aria-label",
        date.toLocaleDateString("pt-BR", {
          day: "numeric",
          month: "long",
          year: "numeric",
        }),
      );
      button.setAttribute("aria-pressed", String(key === selected));
      button.addEventListener("click", () => onDate(key));
      $("calendar-grid").append(button);
    }
  }

  function showDetails(id) {
    const row = rows.find((item) => item.id === id);
    if (!row) return;
    detailId = id;
    const box = element("div", "detail-rows", "");
    const details = [
      ["Cliente", row.cliente],
      ["Telefone", row.telefone],
      ["Serviço", row.servico],
      ["Barbeiro", row.barbeiro],
      ["Data", displayDate(row.date)],
      ["Horário", `${row.time} — ${row.endTime}`],
      ["Valor", money(row.preco)],
    ];
    details.forEach(([label, value]) => {
      const line = element("div", "detail-row", "");
      line.append(element("span", "", label), element("strong", "", value));
      box.append(line);
    });
    $("booking-details").replaceChildren(box);
    $("cancel-booking").disabled = false;
    $("details-dialog").showModal();
  }

  function render() {
    const dayRows = rows
      .filter((row) => row.date === selected)
      .sort((a, b) => a.inicio.localeCompare(b.inicio));
    const totalMinutes = dayRows.reduce((sum, row) => sum + row.duracao, 0);
    $("stat-bookings").textContent = String(dayRows.length).padStart(2, "0");
    $("stat-time").textContent =
      `${Math.floor(totalMinutes / 60)}h${totalMinutes % 60 ? String(totalMinutes % 60).padStart(2, "0") : ""}`;
    $("stat-value").textContent = compactMoney(
      dayRows.reduce((sum, row) => sum + Number(row.preco), 0),
    );
    $("selected-date-label").textContent = new Date(
      `${selected}T12:00:00`,
    ).toLocaleDateString("pt-BR", {
      weekday: "long",
      day: "numeric",
      month: "long",
      year: "numeric",
    });

    const query = $("search").value.trim().toLocaleLowerCase("pt-BR");
    const visible = dayRows.filter(
      (row) =>
        (barber === "todos" || row.barbeiroId === barber) &&
        `${row.cliente} ${row.servico}`
          .toLocaleLowerCase("pt-BR")
          .includes(query),
    );
    $("result-count").textContent =
      `${visible.length} ${visible.length === 1 ? "atendimento" : "atendimentos"}`;
    $("footer-count").textContent = `${catalog.barbeiros.length} profissionais`;
    $("appointments").replaceChildren();
    visible.forEach((row, index) => {
      const card = element("article", "appointment", "");
      const when = element("div", "time", "");
      when.append(
        element("strong", "", row.time),
        element("small", "", `${row.duracao} min`),
      );
      const customer = element("div", "customer", "");
      const text = element("div", "customer-text", "");
      text.append(
        element("strong", "", row.cliente),
        element("small", "", row.servico),
      );
      customer.append(
        element(
          "span",
          `avatar ${["sage", "sand", "blue", "clay"][index % 4]}`,
          initials(row.cliente),
        ),
        text,
      );
      const details = element("button", "details-button", "•••");
      details.type = "button";
      details.setAttribute("aria-label", `Ver detalhes de ${row.cliente}`);
      details.addEventListener("click", () => showDetails(row.id));
      card.append(
        when,
        customer,
        element("span", "barber-name", row.barbeiro),
        element("span", "price", compactMoney(row.preco)),
        details,
      );
      $("appointments").append(card);
    });
    if (!visible.length) {
      const empty = element("div", "empty-state", "");
      empty.append(
        element("strong", "", "Um espaço na agenda."),
        element("p", "", "Nenhum atendimento encontrado para esta consulta."),
      );
      const add = element("button", "button primary", "＋ Novo agendamento");
      add.disabled = !ready;
      add.addEventListener("click", () => openBooking());
      empty.append(add);
      $("appointments").append(empty);
    }

    const next = dayRows[0];
    $("next-appointment").replaceChildren();
    if (next) {
      $("next-appointment").append(
        element("div", "next-time", next.time),
        element("p", "next-name", next.cliente),
        element(
          "p",
          "next-description",
          `${next.servico} · ${next.barbeiro}\n${next.duracao} minutos de cuidado`,
        ),
      );
      const button = element("button", "", "Ver agendamento ↗");
      button.addEventListener("click", () => showDetails(next.id));
      $("next-appointment").append(button);
    } else {
      $("next-appointment").append(
        element("p", "next-name", "Seu próximo encontro"),
        element(
          "p",
          "next-description",
          "Reserve o primeiro horário deste dia.",
        ),
      );
      const button = element("button", "", "Novo agendamento ↗");
      button.disabled = !ready;
      button.addEventListener("click", () => openBooking());
      $("next-appointment").append(button);
    }
    renderCalendar();
  }

  async function refreshBookings() {
    const token = ++listRequest;
    try {
      const data = await NavalhaApi.list();
      if (token !== listRequest) return;
      rows = data.map((row) => ({
        ...row,
        date: row.inicio.split("T")[0],
        time: row.inicio.split("T")[1].slice(0, 5),
        endTime: row.fim.split("T")[1].slice(0, 5),
      }));
      connection(true);
      render();
    } catch (error) {
      if (token !== listRequest) return;
      connection(false);
      throw error;
    }
  }

  async function refreshSlots() {
    const token = ++slotsRequest;
    $("booking-time").value = "";
    $("confirm-booking").disabled = true;
    $("time-slots").replaceChildren(
      element("p", "slots-empty", "Consultando horários disponíveis…"),
    );
    const item = catalog.servicos.find(
      (item) => item.id === $("booking-service").value,
    );
    if (!item) return;
    $("booking-summary").textContent = `${item.nome} · ${item.minutos} minutos`;
    $("booking-price").textContent = money(item.preco);
    const date = $("booking-date").value;
    if (!date) {
      $("time-slots").replaceChildren(
        element("p", "slots-empty", "Escolha uma data."),
      );
      return;
    }
    try {
      const slots = await NavalhaApi.slots(
        date,
        item.id,
        $("booking-barber").value,
      );
      if (token !== slotsRequest) return;
      $("time-slots").replaceChildren();
      slots.forEach((time) => {
        const button = element("button", "time-slot", time);
        button.type = "button";
        button.setAttribute("aria-pressed", "false");
        button.addEventListener("click", () => {
          $("time-slots")
            .querySelectorAll("button")
            .forEach((other) => {
              other.classList.remove("chosen");
              other.setAttribute("aria-pressed", "false");
            });
          button.classList.add("chosen");
          button.setAttribute("aria-pressed", "true");
          $("booking-time").value = time;
          $("form-error").textContent = "";
          $("confirm-booking").disabled = saving;
        });
        $("time-slots").append(button);
      });
      if (!slots.length)
        $("time-slots").append(
          element(
            "p",
            "slots-empty",
            "Sem horários disponíveis. Escolha outra data ou profissional.",
          ),
        );
    } catch (error) {
      if (token !== slotsRequest) return;
      $("time-slots").replaceChildren(
        element(
          "p",
          "slots-empty",
          "Não foi possível carregar os horários. Altere a data para tentar novamente.",
        ),
      );
      throw error;
    }
  }

  function safely(action) {
    Promise.resolve()
      .then(action)
      .catch((error) => notify(error.message));
  }

  function openBooking(serviceId, barberId) {
    if (!ready) return;
    $("booking-form").reset();
    $("form-error").textContent = "";
    $("booking-date").min = today();
    $("booking-date").value = selected < today() ? today() : selected;
    if (serviceId) $("booking-service").value = serviceId;
    if (barberId) $("booking-barber").value = barberId;
    $("booking-dialog").showModal();
    safely(refreshSlots);
  }

  function renderCatalog() {
    $("barber-filters").replaceChildren();
    [{ id: "todos", nome: "Todos os barbeiros" }, ...catalog.barbeiros].forEach(
      (item) => {
        const button = element("button", "filter-chip", item.nome);
        button.type = "button";
        button.dataset.barber = item.id;
        button.classList.toggle("selected", item.id === barber);
        button.setAttribute("aria-pressed", String(item.id === barber));
        $("barber-filters").append(button);
      },
    );
    $("booking-service").replaceChildren();
    $("service-grid").replaceChildren();
    const descriptions = {
      CORTE:
        "Um corte que respeita seu estilo. Acabamento preciso, do clássico ao contemporâneo.",
      BARBA:
        "Desenho, alinhamento e cuidado. Os detalhes que fazem a diferença no seu dia.",
      COMBO:
        "A experiência completa da Navalha. Um tempo para renovar o visual e cuidar de você.",
    };
    catalog.servicos.forEach((item) => {
      $("booking-service").add(new Option(item.nome, item.id));
      const card = element("article", "feature-card", "");
      card.append(
        element("span", "feature-icon", item.id === "COMBO" ? "✧" : "✂"),
        element("h2", "", item.nome),
        element(
          "p",
          "",
          descriptions[item.id] || "Um atendimento pensado para você.",
        ),
      );
      const price = element(
        "div",
        "feature-price",
        `${compactMoney(item.preco)} `,
      );
      price.append(element("small", "", `/ ${item.minutos} minutos`));
      const book = element("button", "button primary", "Agendar serviço ↗");
      book.addEventListener("click", () => openBooking(item.id));
      card.append(price, book);
      $("service-grid").append(card);
    });
    $("booking-barber").replaceChildren();
    $("team-grid").replaceChildren();
    catalog.barbeiros.forEach((item) => {
      $("booking-barber").add(new Option(item.nome, item.id));
      const card = element("article", "feature-card", "");
      card.append(
        element("span", "team-avatar", initials(item.nome)),
        element("h2", "", item.nome),
        element(
          "p",
          "",
          "Atenção aos detalhes e cuidado em cada atendimento. Escolha seu horário com este profissional.",
        ),
      );
      const button = element(
        "button",
        "button primary team-booking",
        `Reservar com ${item.nome}`,
      );
      button.addEventListener("click", () => openBooking(undefined, item.id));
      card.append(button);
      $("team-grid").append(card);
    });
  }

  $("booking-form").addEventListener("submit", async (event) => {
    event.preventDefault();
    if (saving) return;
    if (!$("booking-time").value) {
      $("form-error").textContent = "Selecione um horário disponível.";
      return;
    }
    saving = true;
    $("confirm-booking").disabled = true;
    $("form-error").textContent = "";
    const data = new FormData(event.target);
    let saved = false;
    try {
      const result = await NavalhaApi.create({
        cliente: data.get("name"),
        telefone: data.get("phone"),
        servico: data.get("service"),
        barbeiro: data.get("barber"),
        data: data.get("date"),
        horario: data.get("time"),
      });
      saved = true;
      $("booking-dialog").close();
      barber = "todos";
      $("search").value = "";
      $("barber-filters")
        .querySelectorAll("button")
        .forEach((button) => {
          button.classList.toggle(
            "selected",
            button.dataset.barber === "todos",
          );
          button.setAttribute(
            "aria-pressed",
            String(button.dataset.barber === "todos"),
          );
        });
      onDate(result.inicio.split("T")[0]);
      selectSection("agenda");
      await refreshBookings();
      notify("Agendamento confirmado e salvo no banco.");
    } catch (error) {
      if (saved) {
        notify(
          "Reserva salva no banco, mas a lista não atualizou. Recarregue a página para consultar.",
        );
      } else {
        const message = error.message;
        try {
          await refreshSlots();
        } catch {
          /* O erro original permanece visível. */
        }
        $("form-error").textContent = message;
      }
    } finally {
      saving = false;
      $("confirm-booking").disabled = !$("booking-time").value;
    }
  });

  $("cancel-booking").addEventListener("click", async () => {
    if (canceling) return;
    const row = rows.find((item) => item.id === detailId);
    if (
      !row ||
      !confirm(
        `Cancelar o agendamento de ${row.cliente} em ${displayDate(row.date)} às ${row.time}?`,
      )
    )
      return;
    canceling = true;
    $("cancel-booking").disabled = true;
    let canceled = false;
    try {
      await NavalhaApi.cancel(row.id);
      canceled = true;
      $("details-dialog").close();
      await refreshBookings();
      notify("Agendamento cancelado no banco. O horário foi liberado.");
    } catch (error) {
      notify(
        canceled
          ? "Cancelamento salvo. Recarregue para atualizar a lista."
          : error.message,
      );
    } finally {
      canceling = false;
      $("cancel-booking").disabled = false;
    }
  });

  $("new-booking").addEventListener("click", () => openBooking());
  ["booking-date", "booking-service", "booking-barber"].forEach((id) => {
    $(id).addEventListener("change", () => safely(refreshSlots));
  });
  document
    .querySelectorAll(".close-dialog")
    .forEach((button) =>
      button.addEventListener("click", () => button.closest("dialog").close()),
    );
  $("search").addEventListener("input", render);
  $("barber-filters").addEventListener("click", (event) => {
    const button = event.target.closest("[data-barber]");
    if (!button) return;
    barber = button.dataset.barber;
    $("barber-filters")
      .querySelectorAll("button")
      .forEach((other) => {
        other.classList.toggle("selected", other === button);
        other.setAttribute("aria-pressed", String(other === button));
      });
    savePreferences();
    render();
  });
  function moveDay(delta) {
    const date = new Date(`${selected}T12:00:00`);
    date.setDate(date.getDate() + delta);
    onDate(dayKey(date));
  }
  $("previous-day").addEventListener("click", () => moveDay(-1));
  $("next-day").addEventListener("click", () => moveDay(1));
  $("today").addEventListener("click", () => onDate(today()));
  $("previous-month").addEventListener("click", () => {
    month.setMonth(month.getMonth() - 1);
    renderCalendar();
  });
  $("next-month").addEventListener("click", () => {
    month.setMonth(month.getMonth() + 1);
    renderCalendar();
  });
  document
    .querySelectorAll("[data-section]")
    .forEach((button) =>
      button.addEventListener("click", () =>
        selectSection(button.dataset.section),
      ),
    );

  async function initialize() {
    try {
      catalog = await NavalhaApi.catalog();
      if (
        barber !== "todos" &&
        !catalog.barbeiros.some((item) => item.id === barber)
      )
        barber = "todos";
      ready = true;
      renderCatalog();
      $("new-booking").disabled = false;
      await refreshBookings();
    } catch (error) {
      connection(false);
      if (!rows.length) {
        const empty = element("div", "empty-state", "");
        empty.append(
          element("strong", "", "Não foi possível carregar a agenda."),
          element("p", "", error.message),
        );
        const retry = element("button", "button primary", "Tentar novamente");
        retry.addEventListener("click", initialize);
        empty.append(retry);
        $("appointments").replaceChildren(empty);
      }
      notify(error.message);
    }
  }

  document.addEventListener("visibilitychange", () => {
    if (!document.hidden && ready) safely(refreshBookings);
  });
  setInterval(() => {
    if (
      !document.hidden &&
      ready &&
      !$("booking-dialog").open &&
      !$("details-dialog").open
    )
      safely(refreshBookings);
  }, 30000);
  initialize();
})();
