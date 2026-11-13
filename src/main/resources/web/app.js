// Apresentação: coleta entradas, chama HTTP e renderiza. Regras ficam no Java.
const $ = id => document.getElementById(id);
let catalogo;
let requestSlots = 0;
let requestList = 0;
const reais = n => Number(n).toLocaleString('pt-BR', {style:'currency', currency:'BRL'});
function avisar(texto) { $('mensagem').textContent = texto; $('mensagem').hidden = false; }
async function api(path, options) {
  const response = await fetch('/api/' + path, options);
  if (response.status === 204) return null;
  const body = await response.json();
  if (!response.ok) throw new Error(body.mensagem || 'Não foi possível processar a solicitação.');
  return body;
}
async function executar(action) { try { await action(); } catch(e) { avisar(e.message); } }
function node(tag, classe, texto) { const n = document.createElement(tag); n.className = classe; n.textContent = texto; return n; }
async function horarios() {
  const token = ++requestSlots;
  $('horario').replaceChildren(new Option('Carregando...', ''));
  $('salvar').disabled = true;
  const servico = catalogo.servicos.find(s => s.id === $('servico').value);
  $('resumo').textContent = `${servico.minutos} minutos · ${reais(servico.preco)}`;
  const query = new URLSearchParams({data:$('data').value, servico:$('servico').value, barbeiro:$('barbeiro').value});
  const livres = await api('horarios?' + query);
  if (token !== requestSlots) return;
  $('horario').replaceChildren(new Option(livres.length ? 'Selecione um horário' : 'Sem horários disponíveis', ''));
  livres.forEach(h => $('horario').add(new Option(h,h)));
  $('salvar').disabled = !livres.length;
}
async function listar() {
  const token = ++requestList;
  const rows = await api('agendamentos?' + new URLSearchParams({data:$('filtro').value}));
  if (token !== requestList) return;
  $('contagem').textContent = `${rows.length} agendamento(s)`;
  $('lista').replaceChildren();
  if (!rows.length) $('lista').append(node('p','empty','A cadeira está livre. Nenhum agendamento nesta consulta.'));
  rows.forEach(a => {
    const card = node('article','appointment','');
    const data = a.inicio.split('T');
    const when = node('div','when','');
    when.append(node('strong','',data[1]),node('span','',data[0].split('-').reverse().join('/')));
    const detail = node('div','detail','');
    detail.append(node('h3','',a.cliente),node('p','',`${a.servico} · ${a.barbeiro}`),node('small','',`${a.telefone} · Até ${a.fim.split('T')[1]} · ${reais(a.preco)}`));
    const cancel = node('button','cancel','Cancelar');
    cancel.type = 'button';
    cancel.addEventListener('click', () => executar(async () => {
      if (!confirm(`Cancelar o agendamento de ${a.cliente} às ${data[1]}?`)) return;
      await api('agendamentos/' + a.id, {method:'DELETE'});
      avisar('Agendamento cancelado.'); await listar(); await horarios();
    }));
    card.append(when, detail, cancel); $('lista').append(card);
  });
}
$('form').addEventListener('submit', event => {
  event.preventDefault();
  executar(async () => {
    $('salvar').disabled = true;
    try {
      await api('agendamentos', {method:'POST', body:new URLSearchParams(new FormData($('form')))});
      $('filtro').value = $('data').value; $('cliente').value = ''; $('telefone').value = '';
      avisar('Agendamento confirmado.'); await listar();
    } finally { await horarios(); }
  });
});
['data','servico','barbeiro'].forEach(id => $(id).addEventListener('change', () => executar(horarios)));
$('filtro').addEventListener('change', () => executar(listar));
$('todos').addEventListener('click', () => { $('filtro').value = ''; executar(listar); });
executar(async () => {
  catalogo = await api('catalogo');
  catalogo.servicos.forEach(s => $('servico').add(new Option(`${s.nome} — ${reais(s.preco)}`,s.id)));
  catalogo.barbeiros.forEach(b => $('barbeiro').add(new Option(b.nome,b.id)));
  const hoje = new Intl.DateTimeFormat('sv-SE',{timeZone:'America/Sao_Paulo',year:'numeric',month:'2-digit',day:'2-digit'}).format(new Date());
  $('data').value = hoje; $('filtro').value = hoje;
  await horarios(); await listar();
});
