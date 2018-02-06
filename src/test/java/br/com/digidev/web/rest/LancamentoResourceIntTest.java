package br.com.digidev.web.rest;

import br.com.digidev.FinapplicationApp;

import br.com.digidev.domain.Lancamento;
import br.com.digidev.domain.User;
import br.com.digidev.repository.LancamentoRepository;
import br.com.digidev.service.LancamentoService;
import br.com.digidev.service.UserService;
import br.com.digidev.service.dto.LancamentoDTO;
import br.com.digidev.service.mapper.LancamentoMapper;
import br.com.digidev.service.mapper.UserMapper;
import br.com.digidev.web.rest.errors.ExceptionTranslator;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.digidev.domain.enumeration.Tipo;
/**
 * Test class for the LancamentoResource REST controller.
 *
 * @see LancamentoResource
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = FinapplicationApp.class)
@WithMockUser(username="test",authorities={"ROLE_USER"})
public class LancamentoResourceIntTest {

    private static final Tipo DEFAULT_TIPO = Tipo.RECEITA;
    private static final Tipo UPDATED_TIPO = Tipo.DESPESA;

    private static final LocalDate DEFAULT_DATA = LocalDate.ofEpochDay(0L);
    private static final LocalDate UPDATED_DATA = LocalDate.now(ZoneId.systemDefault());

    private static final String DEFAULT_DESCRICAO = "AAAAAAAAAA";
    private static final String UPDATED_DESCRICAO = "BBBBBBBBBB";

    private static final BigDecimal DEFAULT_VALOR = new BigDecimal(1);
    private static final BigDecimal UPDATED_VALOR = new BigDecimal(2);

    private static final Boolean DEFAULT_PAGO_RECEBIDO = false;
    private static final Boolean UPDATED_PAGO_RECEBIDO = true;

    @Autowired
    private LancamentoRepository lancamentoRepository;

    @Autowired
    private LancamentoMapper lancamentoMapper;

    @Autowired
    private LancamentoService lancamentoService;

    @Autowired
    private MappingJackson2HttpMessageConverter jacksonMessageConverter;

    @Autowired
    private PageableHandlerMethodArgumentResolver pageableArgumentResolver;

    @Autowired
    private ExceptionTranslator exceptionTranslator;

    @Autowired
    private EntityManager em;

    private MockMvc restLancamentoMockMvc;

    private Lancamento lancamento;

    @Autowired
    private UserService userService;

    @Autowired
    private UserMapper userMapper;

    private User user;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        final LancamentoResource lancamentoResource = new LancamentoResource(lancamentoService, userService);
        this.restLancamentoMockMvc = MockMvcBuilders.standaloneSetup(lancamentoResource)
            .setCustomArgumentResolvers(pageableArgumentResolver)
            .setControllerAdvice(exceptionTranslator)
            .setMessageConverters(jacksonMessageConverter).build();
    }

    /**
     * Create an entity for this test.
     *
     * This is a static method, as tests for other entities might also need it,
     * if they test an entity which requires the current entity.
     */
    public static Lancamento createEntity(EntityManager em) {
        Lancamento lancamento = new Lancamento()
            .tipo(DEFAULT_TIPO)
            .data(DEFAULT_DATA)
            .descricao(DEFAULT_DESCRICAO)
            .valor(DEFAULT_VALOR)
            .pagoRecebido(DEFAULT_PAGO_RECEBIDO);
        return lancamento;
    }

    @Before
    public void initTest() {
        lancamento = createEntity(em);
    }

    @Before
    public void createUserTest() {
        user = TestUtil.createEntityUser();
        user = userService.createUser(userMapper.userToUserDTO(user));
    }

    @After
    public void finishRemoveUserTest() {
        userService.deleteUser(user.getLogin());
    }

    @Test
    @Transactional
    public void createLancamento() throws Exception {
        int databaseSizeBeforeCreate = lancamentoRepository.findAll().size();

        // Create the Lancamento
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);
        restLancamentoMockMvc.perform(post("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isCreated());

        // Validate the Lancamento in the database
        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeCreate + 1);
        Lancamento testLancamento = lancamentoList.get(lancamentoList.size() - 1);
        assertThat(testLancamento.getTipo()).isEqualTo(DEFAULT_TIPO);
        assertThat(testLancamento.getData()).isEqualTo(DEFAULT_DATA);
        assertThat(testLancamento.getDescricao()).isEqualTo(DEFAULT_DESCRICAO);
        assertThat(testLancamento.getValor()).isEqualTo(DEFAULT_VALOR);
        assertThat(testLancamento.isPagoRecebido()).isEqualTo(DEFAULT_PAGO_RECEBIDO);
    }

    @Test
    @Transactional
    public void createLancamentoWithExistingId() throws Exception {
        int databaseSizeBeforeCreate = lancamentoRepository.findAll().size();

        // Create the Lancamento with an existing ID
        lancamento.setId(1L);
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);

        // An entity with an existing ID cannot be created, so this API call must fail
        restLancamentoMockMvc.perform(post("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isBadRequest());

        // Validate the Lancamento in the database
        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeCreate);
    }

    @Test
    @Transactional
    public void checkTipoIsRequired() throws Exception {
        int databaseSizeBeforeTest = lancamentoRepository.findAll().size();
        // set the field null
        lancamento.setTipo(null);

        // Create the Lancamento, which fails.
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);

        restLancamentoMockMvc.perform(post("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isBadRequest());

        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDataIsRequired() throws Exception {
        int databaseSizeBeforeTest = lancamentoRepository.findAll().size();
        // set the field null
        lancamento.setData(null);

        // Create the Lancamento, which fails.
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);

        restLancamentoMockMvc.perform(post("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isBadRequest());

        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkDescricaoIsRequired() throws Exception {
        int databaseSizeBeforeTest = lancamentoRepository.findAll().size();
        // set the field null
        lancamento.setDescricao(null);

        // Create the Lancamento, which fails.
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);

        restLancamentoMockMvc.perform(post("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isBadRequest());

        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void checkValorIsRequired() throws Exception {
        int databaseSizeBeforeTest = lancamentoRepository.findAll().size();
        // set the field null
        lancamento.setValor(null);

        // Create the Lancamento, which fails.
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);

        restLancamentoMockMvc.perform(post("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isBadRequest());

        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeTest);
    }

    @Test
    @Transactional
    public void getAllLancamentos() throws Exception {
        // Initialize the database
        lancamento.setUser(user);
        lancamentoRepository.saveAndFlush(lancamento);

        // Get all the lancamentoList
        restLancamentoMockMvc.perform(get("/api/lancamentos?sort=id,desc"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.[*].id").value(hasItem(lancamento.getId().intValue())))
            .andExpect(jsonPath("$.[*].tipo").value(hasItem(DEFAULT_TIPO.toString())))
            .andExpect(jsonPath("$.[*].data").value(hasItem(DEFAULT_DATA.toString())))
            .andExpect(jsonPath("$.[*].descricao").value(hasItem(DEFAULT_DESCRICAO.toString())))
            .andExpect(jsonPath("$.[*].valor").value(hasItem(DEFAULT_VALOR.intValue())))
            .andExpect(jsonPath("$.[*].pagoRecebido").value(hasItem(DEFAULT_PAGO_RECEBIDO.booleanValue())));
    }

    @Test
    @Transactional
    public void getLancamento() throws Exception {
        // Initialize the database
        lancamento.setUser(user);
        lancamentoRepository.saveAndFlush(lancamento);

        // Get the lancamento
        restLancamentoMockMvc.perform(get("/api/lancamentos/{id}", lancamento.getId()))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON_UTF8_VALUE))
            .andExpect(jsonPath("$.id").value(lancamento.getId().intValue()))
            .andExpect(jsonPath("$.tipo").value(DEFAULT_TIPO.toString()))
            .andExpect(jsonPath("$.data").value(DEFAULT_DATA.toString()))
            .andExpect(jsonPath("$.descricao").value(DEFAULT_DESCRICAO.toString()))
            .andExpect(jsonPath("$.valor").value(DEFAULT_VALOR.intValue()))
            .andExpect(jsonPath("$.pagoRecebido").value(DEFAULT_PAGO_RECEBIDO.booleanValue()));
    }

    @Test
    @Transactional
    public void getNonExistingLancamento() throws Exception {
        // Get the lancamento
        restLancamentoMockMvc.perform(get("/api/lancamentos/{id}", Long.MAX_VALUE))
            .andExpect(status().isNotFound());
    }

    @Test
    @Transactional
    public void updateLancamento() throws Exception {
        // Initialize the database
        lancamentoRepository.saveAndFlush(lancamento);
        int databaseSizeBeforeUpdate = lancamentoRepository.findAll().size();

        // Update the lancamento
        Lancamento updatedLancamento = lancamentoRepository.findOne(lancamento.getId());
        updatedLancamento
            .tipo(UPDATED_TIPO)
            .data(UPDATED_DATA)
            .descricao(UPDATED_DESCRICAO)
            .valor(UPDATED_VALOR)
            .pagoRecebido(UPDATED_PAGO_RECEBIDO);
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(updatedLancamento);
        lancamentoDTO.setUserId(user.getId());

        restLancamentoMockMvc.perform(put("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isOk());

        // Validate the Lancamento in the database
        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeUpdate);
        Lancamento testLancamento = lancamentoList.get(lancamentoList.size() - 1);
        assertThat(testLancamento.getTipo()).isEqualTo(UPDATED_TIPO);
        assertThat(testLancamento.getData()).isEqualTo(UPDATED_DATA);
        assertThat(testLancamento.getDescricao()).isEqualTo(UPDATED_DESCRICAO);
        assertThat(testLancamento.getValor()).isEqualTo(UPDATED_VALOR);
        assertThat(testLancamento.isPagoRecebido()).isEqualTo(UPDATED_PAGO_RECEBIDO);
    }

    @Test
    @Transactional
    public void updateNonExistingLancamento() throws Exception {
        int databaseSizeBeforeUpdate = lancamentoRepository.findAll().size();

        // Create the Lancamento
        LancamentoDTO lancamentoDTO = lancamentoMapper.toDto(lancamento);

        // If the entity doesn't have an ID, it will be created instead of just being updated
        restLancamentoMockMvc.perform(put("/api/lancamentos")
            .contentType(TestUtil.APPLICATION_JSON_UTF8)
            .content(TestUtil.convertObjectToJsonBytes(lancamentoDTO)))
            .andExpect(status().isCreated());

        // Validate the Lancamento in the database
        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeUpdate + 1);
    }

    @Test
    @Transactional
    public void deleteLancamento() throws Exception {
        // Initialize the database
        lancamento.setUser(user);
        lancamentoRepository.saveAndFlush(lancamento);
        int databaseSizeBeforeDelete = lancamentoRepository.findAll().size();

        // Get the lancamento
        restLancamentoMockMvc.perform(delete("/api/lancamentos/{id}", lancamento.getId())
            .accept(TestUtil.APPLICATION_JSON_UTF8))
            .andExpect(status().isOk());

        // Validate the database is empty
        List<Lancamento> lancamentoList = lancamentoRepository.findAll();
        assertThat(lancamentoList).hasSize(databaseSizeBeforeDelete - 1);
    }

    @Test
    @Transactional
    public void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Lancamento.class);
        Lancamento lancamento1 = new Lancamento();
        lancamento1.setId(1L);
        Lancamento lancamento2 = new Lancamento();
        lancamento2.setId(lancamento1.getId());
        assertThat(lancamento1).isEqualTo(lancamento2);
        lancamento2.setId(2L);
        assertThat(lancamento1).isNotEqualTo(lancamento2);
        lancamento1.setId(null);
        assertThat(lancamento1).isNotEqualTo(lancamento2);
    }

    @Test
    @Transactional
    public void dtoEqualsVerifier() throws Exception {
        TestUtil.equalsVerifier(LancamentoDTO.class);
        LancamentoDTO lancamentoDTO1 = new LancamentoDTO();
        lancamentoDTO1.setId(1L);
        LancamentoDTO lancamentoDTO2 = new LancamentoDTO();
        assertThat(lancamentoDTO1).isNotEqualTo(lancamentoDTO2);
        lancamentoDTO2.setId(lancamentoDTO1.getId());
        assertThat(lancamentoDTO1).isEqualTo(lancamentoDTO2);
        lancamentoDTO2.setId(2L);
        assertThat(lancamentoDTO1).isNotEqualTo(lancamentoDTO2);
        lancamentoDTO1.setId(null);
        assertThat(lancamentoDTO1).isNotEqualTo(lancamentoDTO2);
    }

    @Test
    @Transactional
    public void testEntityFromId() {
        assertThat(lancamentoMapper.fromId(42L).getId()).isEqualTo(42);
        assertThat(lancamentoMapper.fromId(null)).isNull();
    }
}
