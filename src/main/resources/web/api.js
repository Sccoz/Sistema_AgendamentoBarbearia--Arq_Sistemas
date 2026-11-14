/** Adaptador HTTP: o frontend conversa somente com a API Java. */
const NavalhaApi = (() => {
  async function request(path, options = {}) {
    let response;
    try {
      response = await fetch(`/api/${path}`, { cache: "no-store", ...options });
    } catch {
      throw new Error(
        "Não foi possível acessar o servidor. Verifique se o Java está executando.",
      );
    }

    if (response.status === 204) return null;

    let body;
    try {
      body = await response.json();
    } catch {
      throw new Error(
        "O servidor retornou uma resposta inválida. Tente novamente.",
      );
    }

    if (!response.ok) {
      throw new Error(
        body.mensagem || "Não foi possível concluir a solicitação.",
      );
    }
    return body;
  }

  return Object.freeze({
    catalog: () => request("catalogo"),
    list: () => request("agendamentos"),
    slots: (date, serviceId, barberId) =>
      request(
        `horarios?${new URLSearchParams({
          data: date,
          servico: serviceId,
          barbeiro: barberId,
        })}`,
      ),
    create: (data) =>
      request("agendamentos", {
        method: "POST",
        body: new URLSearchParams(data),
      }),
    cancel: (id) =>
      request(`agendamentos/${encodeURIComponent(id)}`, { method: "DELETE" }),
  });
})();
